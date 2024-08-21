package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.location.countries.CountryCreateException;
import com.machines.machines_api.exceptions.location.countries.CountryNotFoundException;
import com.machines.machines_api.models.dto.request.CountryRequestDTO;
import com.machines.machines_api.models.dto.response.CountryResponseDTO;
import com.machines.machines_api.models.dto.response.admin.CountryAdminResponseDTO;
import com.machines.machines_api.models.entity.Country;
import com.machines.machines_api.repositories.CountryRepository;
import com.machines.machines_api.services.CountryService;
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
public class CountryServiceImpl implements CountryService {
    private final CountryRepository countryRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CountryResponseDTO> getAll(boolean includeRegions) {
        return countryRepository
                .findAllByDeletedAtIsNull()
                .stream()
                .map(x -> {
                    if (!includeRegions) {
                        x.setRegions(null);
                    }

                    return modelMapper.map(x, CountryResponseDTO.class);
                })
                .toList();
    }

    @Override
    public List<CountryAdminResponseDTO> getAllAdmin(boolean includeRegions) {
        return countryRepository
                .findAll()
                .stream()
                .map(x -> {
                    if (!includeRegions) {
                        x.setRegions(null);
                    }

                    return modelMapper.map(x, CountryAdminResponseDTO.class);
                })
                .toList();
    }


    @Override
    public CountryResponseDTO getById(UUID id) {
        return modelMapper.map(getEntityById(id), CountryResponseDTO.class);
    }

    @Override
    public CountryAdminResponseDTO getCountryByIdAdmin(UUID id) {
        return modelMapper.map(getEntityByIdAdmin(id), CountryAdminResponseDTO.class);
    }

    @Override
    public CountryResponseDTO create(CountryRequestDTO countryRequestDTO) {
        if (countryRepository.findByNameAndDeletedAtIsNull(countryRequestDTO.getName()).isPresent()) {
            throw new CountryCreateException(true);
        }

        Country country = modelMapper.map(countryRequestDTO, Country.class);

        try {
            Country savedCountry = countryRepository.save(country);
            savedCountry.setRegions(new ArrayList<>());

            return modelMapper.map(savedCountry, CountryResponseDTO.class);
        } catch (RuntimeException exception) {
            throw new CountryCreateException(false);
        }
    }

    @Override
    public CountryResponseDTO update(UUID id, CountryRequestDTO countryRequestDTO) {
        Country country = getEntityByIdAdmin(id);
        Optional<Country> potentialCountry = countryRepository.findByNameAndDeletedAtIsNull(countryRequestDTO.getName());

        if (potentialCountry.isPresent() && !country.getId().equals(potentialCountry.get().getId())) {
            throw new CountryCreateException(true);
        }

        if (countryRequestDTO.getName() != null) {
            country.setName(countryRequestDTO.getName());
        }

        Country savedCountry = countryRepository.save(country);
        return modelMapper.map(savedCountry, CountryResponseDTO.class);
    }

    @Override
    public void delete(UUID id) {
        Country country = getEntityByIdAdmin(id);

        if (country.getDeletedAt() == null) {
            country.setDeletedAt(LocalDateTime.now());
        } else {
            country.setDeletedAt(null);
        }

        countryRepository.save(country);
    }

    @Override
    public Country getEntityById(UUID id) {
        Optional<Country> country = countryRepository.findByIdAndDeletedAtIsNull(id);

        if (country.isEmpty()) {
            throw new CountryNotFoundException();
        }

        return country.get();
    }

    @Override
    public Country getEntityByIdAdmin(UUID id) {
        Optional<Country> country = countryRepository.findById(id);

        if (country.isEmpty()) {
            throw new CountryNotFoundException();
        }

        return country.get();
    }
}
