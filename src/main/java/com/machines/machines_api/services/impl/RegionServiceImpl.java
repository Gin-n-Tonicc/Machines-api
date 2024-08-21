package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.location.regions.RegionCreateException;
import com.machines.machines_api.exceptions.location.regions.RegionNotFoundException;
import com.machines.machines_api.models.dto.request.RegionRequestDTO;
import com.machines.machines_api.models.dto.response.RegionResponseDTO;
import com.machines.machines_api.models.dto.response.admin.RegionAdminResponseDTO;
import com.machines.machines_api.models.entity.Country;
import com.machines.machines_api.models.entity.Region;
import com.machines.machines_api.repositories.RegionRepository;
import com.machines.machines_api.services.CountryService;
import com.machines.machines_api.services.RegionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Component
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {
    private final RegionRepository regionRepository;
    private final CountryService countryService;
    private final ModelMapper modelMapper;

    @Override
    public List<RegionResponseDTO> getAll() {
        return regionRepository
                .findAllByDeletedAtIsNull()
                .stream()
                .map(
                        x -> modelMapper.map(x, RegionResponseDTO.class)
                )
                .toList();
    }

    @Override
    public List<RegionAdminResponseDTO> getAllAdmin() {
        return regionRepository
                .findAll()
                .stream()
                .map(
                        x -> modelMapper.map(x, RegionAdminResponseDTO.class)
                )
                .toList();
    }

    @Override
    public RegionResponseDTO getById(UUID id) {
        return modelMapper.map(getEntityById(id), RegionResponseDTO.class);
    }

    @Override
    public RegionAdminResponseDTO getCountryByIdAdmin(UUID id) {
        return modelMapper.map(getEntityByIdAdmin(id), RegionAdminResponseDTO.class);
    }

    @Override
    public RegionResponseDTO create(RegionRequestDTO regionRequestDTO) {
        if (regionRepository.findByNameAndDeletedAtIsNull(regionRequestDTO.getName()).isPresent()) {
            throw new RegionCreateException(true);
        }

        Country country = countryService.getEntityById(regionRequestDTO.getCountryId());
        Region region = modelMapper.map(regionRequestDTO, Region.class);
        region.setCountry(country);
        region.setId(null);

        try {
            Region savedRegion = regionRepository.save(region);
            savedRegion.setCities(new ArrayList<>());

            return modelMapper.map(savedRegion, RegionResponseDTO.class);
        } catch (RuntimeException e) {
            throw new RegionCreateException(false);
        }
    }

    @Override
    public RegionResponseDTO update(UUID id, RegionRequestDTO regionRequestDTO) {
        Region region = getEntityByIdAdmin(id);
        Optional<Region> potentialRegion = regionRepository.findByNameAndDeletedAtIsNull(regionRequestDTO.getName());

        if (potentialRegion.isPresent() && !region.getId().equals(potentialRegion.get().getId())) {
            throw new RegionCreateException(true);
        }

        if (regionRequestDTO.getName() != null) {
            region.setName(regionRequestDTO.getName());
        }

        if (regionRequestDTO.getCountryId() != null) {
            Country country = countryService.getEntityById(regionRequestDTO.getCountryId());
            region.setCountry(country);
        }

        Region savedRegion = regionRepository.save(region);
        return modelMapper.map(savedRegion, RegionResponseDTO.class);
    }

    @Override
    public void delete(UUID id) {
        Region region = getEntityByIdAdmin(id);

        if (region.getDeletedAt() == null) {
            region.setDeletedAt(LocalDateTime.now());
        } else {
            region.setDeletedAt(null);
        }

        regionRepository.save(region);
    }

    @Override
    public Region getEntityById(UUID id) {
        Optional<Region> region = regionRepository.findByIdAndDeletedAtIsNull(id);

        if (region.isEmpty()) {
            throw new RegionNotFoundException();
        }

        return region.get();
    }

    @Override
    public Region getEntityByIdAdmin(UUID id) {
        Optional<Region> region = regionRepository.findById(id);

        if (region.isEmpty()) {
            throw new RegionNotFoundException();
        }

        return region.get();
    }
}
