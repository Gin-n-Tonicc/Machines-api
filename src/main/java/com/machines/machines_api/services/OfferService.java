package com.machines.machines_api.services;

import com.machines.machines_api.models.dto.auth.PublicUserDTO;
import com.machines.machines_api.models.dto.request.OfferRequestDTO;
import com.machines.machines_api.models.dto.response.OfferResponseDTO;
import com.machines.machines_api.models.entity.Offer;

import java.util.List;
import java.util.UUID;

public interface OfferService {
    List<OfferResponseDTO> getAll();
    OfferResponseDTO getById(UUID id);
    OfferResponseDTO create(OfferRequestDTO offerRequestDTO, PublicUserDTO user);
    OfferResponseDTO update(UUID id, OfferRequestDTO offerRequestDTO, PublicUserDTO user);
    void delete(UUID id, PublicUserDTO user);
    Offer getEntityById(UUID id);
}