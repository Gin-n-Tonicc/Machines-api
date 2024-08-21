package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.location.cities.CityCreateException;
import com.machines.machines_api.exceptions.location.cities.CityNotFoundException;
import com.machines.machines_api.models.dto.request.CityRequestDTO;
import com.machines.machines_api.models.dto.response.CityResponseDTO;
import com.machines.machines_api.models.dto.response.admin.CityAdminResponseDTO;
import com.machines.machines_api.models.entity.City;
import com.machines.machines_api.models.entity.Region;
import com.machines.machines_api.repositories.CityRepository;
import com.machines.machines_api.services.CityService;
import com.machines.machines_api.services.RegionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Component
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {
    private final RegionService regionService;
    private final CityRepository cityRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CityResponseDTO> getAll() {
        return cityRepository
                .findAllByDeletedAtIsNull()
                .stream()
                .map(
                        x -> modelMapper.map(x, CityResponseDTO.class)
                )
                .toList();
    }

    @Override
    public List<CityAdminResponseDTO> getAllAdmin() {
        return cityRepository
                .findAll()
                .stream()
                .map(
                        x -> modelMapper.map(x, CityAdminResponseDTO.class)
                )
                .toList();
    }

    @Override
    public CityResponseDTO getById(UUID id) {
        return modelMapper.map(getEntityById(id), CityResponseDTO.class);
    }

    @Override
    public CityAdminResponseDTO getCategoryByIdAdmin(UUID id) {
        return modelMapper.map(getEntityByIdAdmin(id), CityAdminResponseDTO.class);
    }

    @Override
    public CityResponseDTO create(CityRequestDTO cityRequestDTO) {
        if (cityRepository.findByNameAndDeletedAtIsNull(cityRequestDTO.getName()).isPresent()) {
            throw new CityCreateException(true);
        }

        Region region = regionService.getEntityById(cityRequestDTO.getRegionId());

        City city = modelMapper.map(cityRequestDTO, City.class);
        city.setRegion(region);
        city.setId(null);

        try {
            City savedCity = cityRepository.save(city);
            return modelMapper.map(savedCity, CityResponseDTO.class);
        } catch (Exception e) {
            throw new CityCreateException(false);
        }
    }

    @Override
    public CityResponseDTO update(UUID id, CityRequestDTO cityRequestDTO) {
        City city = getEntityByIdAdmin(id);
        Optional<City> potentialCity = cityRepository.findByNameAndDeletedAtIsNull(cityRequestDTO.getName());

        if (potentialCity.isPresent() && !city.getId().equals(potentialCity.get().getId())) {
            throw new CityCreateException(true);
        }

        if (cityRequestDTO.getRegionId() != null) {
            Region region = regionService.getEntityById(cityRequestDTO.getRegionId());
            city.setRegion(region);
        }

        if (cityRequestDTO.getName() != null) {
            city.setName(cityRequestDTO.getName());
        }

        City savedCity = cityRepository.save(city);
        return modelMapper.map(savedCity, CityResponseDTO.class);
    }

    @Override
    public void delete(UUID id) {
        City city = getEntityByIdAdmin(id);

        if (city.getDeletedAt() == null) {
            city.setDeletedAt(LocalDateTime.now());
        } else {
            city.setDeletedAt(null);
        }

        cityRepository.save(city);
    }

    @Override
    public City getEntityById(UUID id) {
        Optional<City> city = cityRepository.findByIdAndDeletedAtIsNull(id);

        if (city.isEmpty()) {
            throw new CityNotFoundException();
        }

        return city.get();
    }

    @Override
    public City getEntityByIdAdmin(UUID id) {
        Optional<City> city = cityRepository.findById(id);

        if (city.isEmpty()) {
            throw new CityNotFoundException();
        }

        return city.get();
    }
}
