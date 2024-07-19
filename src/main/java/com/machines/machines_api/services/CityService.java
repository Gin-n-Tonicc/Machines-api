package com.machines.machines_api.services;

import com.machines.machines_api.models.dto.request.CityRequestDTO;
import com.machines.machines_api.models.dto.response.CityResponseDTO;
import com.machines.machines_api.models.dto.response.RegionResponseDTO;
import com.machines.machines_api.models.entity.City;

import java.util.List;
import java.util.UUID;

public interface CityService {
    List<CityResponseDTO> getAll();
    CityResponseDTO getById(UUID id);
    CityResponseDTO create(CityRequestDTO cityRequestDTO);
    CityResponseDTO update(UUID id, CityRequestDTO cityRequestDTO);
    void delete(UUID id);
    City getEntityById(UUID id);
}