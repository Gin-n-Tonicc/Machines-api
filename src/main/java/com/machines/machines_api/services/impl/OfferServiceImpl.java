package com.machines.machines_api.services.impl;

import com.machines.machines_api.enums.OfferSort;
import com.machines.machines_api.enums.OfferType;
import com.machines.machines_api.enums.Role;
import com.machines.machines_api.exceptions.common.AccessDeniedException;
import com.machines.machines_api.exceptions.common.BadRequestException;
import com.machines.machines_api.exceptions.offer.OfferNotFoundException;
import com.machines.machines_api.models.dto.auth.PublicUserDTO;
import com.machines.machines_api.models.dto.common.OfferTypeDTO;
import com.machines.machines_api.models.dto.request.OfferRequestDTO;
import com.machines.machines_api.models.dto.request.checkout.BaseCheckoutRequestDTO;
import com.machines.machines_api.models.dto.request.checkout.OfferCheckoutRequestDTO;
import com.machines.machines_api.models.dto.response.OfferResponseDTO;
import com.machines.machines_api.models.dto.response.OfferSingleResponseDTO;
import com.machines.machines_api.models.dto.response.admin.OfferAdminResponseDTO;
import com.machines.machines_api.models.dto.response.admin.OfferSingleAdminResponseDTO;
import com.machines.machines_api.models.dto.specifications.OfferSpecificationDTO;
import com.machines.machines_api.models.entity.*;
import com.machines.machines_api.repositories.OfferRepository;
import com.machines.machines_api.services.*;
import com.machines.machines_api.specifications.OfferSpecification;
import com.stripe.exception.StripeException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Component
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {
    private final UserService userService;
    private final FileService fileService;
    private final CityService cityService;
    private final CheckoutService checkoutService;
    private final SubcategoryService subcategoryService;
    private final OfferRepository offerRepository;
    private final ModelMapper modelMapper;
    private final Validator validator;

    @Override
    public List<OfferTypeDTO> getOfferTypesAsProducts() {
        return OfferType
                .getOfferTypes()
                .stream()
                .map(OfferType::toOfferTypeDTO)
                .toList();
    }

    @Override
    public Page<OfferResponseDTO> getAll(int page, int size, OfferSpecificationDTO offerSpecificationDTO) {
        Specification<Offer> offerSpecification = OfferSpecification.filterOffer(offerSpecificationDTO);
        Sort offerSort = getOfferSort(offerSpecificationDTO.getOfferSort());

        // Page request starts from 0 but actual pages start from 1
        // So if page = 1 then page request should start from 0
        PageRequest pageRequest = PageRequest.of(page - 1, size, offerSort);

        // Handle "deletedAt = null" in the OfferSpecification.java class
        var response = offerRepository.findAll(offerSpecification, pageRequest);
        return response.map(x -> modelMapper.map(x, OfferResponseDTO.class));
    }

    @Override
    public Page<OfferAdminResponseDTO> getAllForLoggedUser(int page, int size, PublicUserDTO user) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        User loggedUser = userService.findByEmail(user.getEmail());

        var response = offerRepository.findAllByOwnerId(loggedUser.getId(), pageRequest);

        return response.map(x -> modelMapper.map(x, OfferAdminResponseDTO.class));
    }

    @Override
    public Page<OfferAdminResponseDTO> getAllAdmin(int page, int size) {
        // Page request starts from 0 but actual pages start from 1
        // So if page = 1 then page request should start from 0
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<Offer> offers = offerRepository.findAll(pageRequest);
        return offers.map(x -> modelMapper.map(x, OfferAdminResponseDTO.class));
    }

    @Override
    public OfferResponseDTO getById(UUID id, PublicUserDTO user) {
        Offer offer = offerRepository.findById(id).orElseThrow(OfferNotFoundException::new);

        if (user != null) {
            User loggedUser = userService.findByEmail(user.getEmail());

            if (loggedUser.getId() == offer.getOwner().getId()) {
                OfferSingleResponseDTO offerSingleResponseDTO = modelMapper.map(offer, OfferSingleResponseDTO.class);

                List<OfferResponseDTO> similarOffers = findSimilarOffers(offerSingleResponseDTO.getTitle(), offerSingleResponseDTO.getId());
                offerSingleResponseDTO.setSimilarOffers(similarOffers);

                return offerSingleResponseDTO;

            }
        }

        OfferSingleResponseDTO offerSingleResponseDTO = modelMapper.map(getEntityById(id), OfferSingleResponseDTO.class);
        List<OfferResponseDTO> similarOffers = findSimilarOffers(offerSingleResponseDTO.getTitle(), offerSingleResponseDTO.getId());
        offerSingleResponseDTO.setSimilarOffers(similarOffers);

        return offerSingleResponseDTO;
    }

    @Override
    public OfferSingleAdminResponseDTO getByIdAdmin(UUID id) {
        OfferSingleAdminResponseDTO offerSingleResponseDTO = modelMapper.map(getEntityByIdAdmin(id), OfferSingleAdminResponseDTO.class);
        List<OfferResponseDTO> similarOffers = findSimilarOffers(offerSingleResponseDTO.getTitle(), offerSingleResponseDTO.getId());
        offerSingleResponseDTO.setSimilarOffers(similarOffers);

        return offerSingleResponseDTO;
    }

    @Override
    public OfferResponseDTO create(OfferRequestDTO offerRequestDTO, PublicUserDTO user) {
        var violations = validator.validate(offerRequestDTO);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        User owner = userService.findById(user.getId());

        Offer offer = modelMapper.map(offerRequestDTO, Offer.class);
        mapRequestDTOIdsToEntities(offerRequestDTO, offer);
        offer.setOwner(owner);

        Offer savedOffer = offerRepository.save(offer);
        return modelMapper.map(savedOffer, OfferResponseDTO.class);
    }

    @Override
    public String createPromoteSession(UUID id, String customerName, OfferType offerType, PublicUserDTO user) throws StripeException {
        Offer offer = getEntityById(id);

        if (!user.getRole().equals(Role.ADMIN)) {
            if (!offer.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException();
            }
        }

        BaseCheckoutRequestDTO baseCheckoutRequestDTO = BaseCheckoutRequestDTO.builder().customerEmail(user.getEmail()).customerName(customerName).build();
        OfferCheckoutRequestDTO offerCheckoutRequestDTO = new OfferCheckoutRequestDTO(offerType, id, baseCheckoutRequestDTO);

        return checkoutService.createPromoteOfferHostedCheckoutSession(offerCheckoutRequestDTO);
    }

    @Override
    public void updateOfferType(UUID id, OfferType offerType) {
        Offer offer = getEntityByIdAdmin(id);
        offer.setOfferType(offerType);
        offer.setPromotedAt(LocalDateTime.now());

        offerRepository.save(offer);
    }

    @Override
    public OfferResponseDTO update(UUID id, OfferRequestDTO offerRequestDTO, PublicUserDTO user) {
        Offer offer = getEntityByIdAdmin(id);

        if (!user.getRole().equals(Role.ADMIN)) {
            if (!offer.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException();
            }
        }

        modelMapper.map(offerRequestDTO, offer);
        mapRequestDTOIdsToEntities(offerRequestDTO, offer);

        Offer updatedOffer = offerRepository.save(offer);
        return modelMapper.map(updatedOffer, OfferResponseDTO.class);
    }

    @Override
    public void delete(UUID id, PublicUserDTO user) {
        Offer offer = getEntityByIdAdmin(id);

        if (!user.getRole().equals(Role.ADMIN)) {
            if (!offer.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException();
            }
        }

        if (offer.getDeletedAt() == null) {
            offer.delete();
        } else {
            offer.setDeletedAt(null);
        }

        offerRepository.save(offer);
    }

    @Override
    public Offer getEntityById(UUID id) {
        Optional<Offer> offer = offerRepository.findByIdAndDeletedAtIsNull(id);

        if (offer.isEmpty()) {
            throw new OfferNotFoundException();
        }

        return offer.get();
    }

    @Override
    public Offer getEntityByIdAdmin(UUID id) {
        Optional<Offer> offer = offerRepository.findById(id);

        if (offer.isEmpty()) {
            throw new OfferNotFoundException();
        }

        return offer.get();
    }

    public List<OfferResponseDTO> findSimilarOffers(String title, UUID id) {
        String searchTerm = String.join(" | ", title.split("\\s+"));

        return offerRepository
                .findSimilarOffers(searchTerm, id)
                .stream()
                .map(x -> modelMapper.map(x, OfferResponseDTO.class))
                .toList();
    }

    public void mapRequestDTOIdsToEntities(OfferRequestDTO offerRequestDTO, Offer offer) {
        if (offerRequestDTO.getSubcategoryId() != null) {
            Subcategory subcategory = subcategoryService.getSubCategoryEntityById(offerRequestDTO.getSubcategoryId());
            offer.setSubcategory(subcategory);
        }

        if (offerRequestDTO.getCityId() != null) {
            City city = cityService.getEntityById(offerRequestDTO.getCityId());
            offer.setCity(city);
        }

        if (offerRequestDTO.getMainPictureId() != null) {
            File mainPicture = fileService.getEntityById(offerRequestDTO.getMainPictureId());
            offer.setMainPicture(mainPicture);
        }

        if (offerRequestDTO.getPictureIds() != null) {
            Set<File> pictures = offerRequestDTO.getPictureIds().stream().map(fileService::getEntityById).collect(Collectors.toSet());
            offer.setPictures(pictures);
        }
    }

    private Sort getOfferSort(OfferSort offerSort) {
        if (offerSort.equals(OfferSort.def)) {
            return Sort.by("createdAt").descending();
        } else if (offerSort.equals(OfferSort.alphaDesc)) {
            return Sort.by("title").ascending();
        }

        throw new BadRequestException("Невалидно сортиране!");
    }
}
