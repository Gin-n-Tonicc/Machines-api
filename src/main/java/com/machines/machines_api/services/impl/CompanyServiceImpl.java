package com.machines.machines_api.services.impl;

import com.machines.machines_api.enums.CompanySort;
import com.machines.machines_api.enums.Role;
import com.machines.machines_api.exceptions.common.AccessDeniedException;
import com.machines.machines_api.exceptions.common.BadRequestException;
import com.machines.machines_api.exceptions.company.CompanyNotFoundException;
import com.machines.machines_api.models.dto.auth.PublicUserDTO;
import com.machines.machines_api.models.dto.request.CompanyRequestDTO;
import com.machines.machines_api.models.dto.response.CompanyResponseDTO;
import com.machines.machines_api.models.dto.response.admin.CompanyAdminResponseDTO;
import com.machines.machines_api.models.dto.specifications.CompanySpecificationDTO;
import com.machines.machines_api.models.entity.City;
import com.machines.machines_api.models.entity.Company;
import com.machines.machines_api.models.entity.File;
import com.machines.machines_api.models.entity.User;
import com.machines.machines_api.repositories.CompanyRepository;
import com.machines.machines_api.services.CityService;
import com.machines.machines_api.services.CompanyService;
import com.machines.machines_api.services.FileService;
import com.machines.machines_api.services.UserService;
import com.machines.machines_api.specifications.CompanySpecification;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Component
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final UserService userService;
    private final FileService fileService;
    private final CityService cityService;
    private final CompanyRepository companyRepository;
    private final ModelMapper modelMapper;
    private final Validator validator;

    @Override
    public Page<CompanyResponseDTO> getAll(int page, int size, CompanySpecificationDTO companySpecificationDTO) {
        Specification<Company> companySpecification = CompanySpecification.filterCompany(companySpecificationDTO);
        Sort companySort = getCompanySort(companySpecificationDTO.getCompanySort());

        // Page request starts from 0 but actual pages start from 1
        // So if page = 1 then page request should start from 0
        PageRequest pageRequest = PageRequest.of(page - 1, size, companySort);

        // Handle "deletedAt = null" in the CompanySpecification.java class
        var response = companyRepository.findAll(companySpecification, pageRequest);
        return response.map(x -> modelMapper.map(x, CompanyResponseDTO.class));
    }

    @Override
    public Page<CompanyAdminResponseDTO> getAllForLoggedUser(int page, int size, PublicUserDTO user) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        User loggedUser = userService.findByEmail(user.getEmail());

        var response = companyRepository.findAllByOwnerId(loggedUser.getId(), pageRequest);

        return response.map(x -> modelMapper.map(x, CompanyAdminResponseDTO.class));
    }

    @Override
    public Page<CompanyAdminResponseDTO> getAllAdmin(int page, int size) {
        // Page request starts from 0 but actual pages start from 1
        // So if page = 1 then page request should start from 0
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<Company> companies = companyRepository.findAll(pageRequest);
        return companies.map(x -> modelMapper.map(x, CompanyAdminResponseDTO.class));
    }

    @Override
    public CompanyResponseDTO getById(UUID id, PublicUserDTO user) {
        Company company = companyRepository.findById(id).orElseThrow(CompanyNotFoundException::new);

        return modelMapper.map(company, CompanyResponseDTO.class);
    }

    @Override
    public CompanyAdminResponseDTO getByIdAdmin(UUID id) {
        return modelMapper.map(getEntityByIdAdmin(id), CompanyAdminResponseDTO.class);
    }

    @Override
    public CompanyResponseDTO create(CompanyRequestDTO companyRequestDTO, PublicUserDTO user) {
        var violations = validator.validate(companyRequestDTO);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        User owner = userService.findById(user.getId());

        Company company = modelMapper.map(companyRequestDTO, Company.class);
        mapRequestDTOIdsToEntities(companyRequestDTO, company);
        company.setOwner(owner);

        Company savedCompany = companyRepository.save(company);
        return modelMapper.map(savedCompany, CompanyResponseDTO.class);
    }

    @Override
    public CompanyResponseDTO update(UUID id, CompanyRequestDTO companyRequestDTO, PublicUserDTO user) {
        Company company = getEntityByIdAdmin(id);

        if (!user.getRole().equals(Role.ADMIN)) {
            if (!company.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException();
            }
        }

        modelMapper.map(companyRequestDTO, company);
        mapRequestDTOIdsToEntities(companyRequestDTO, company);

        Company updatedCompany = companyRepository.save(company);
        return modelMapper.map(updatedCompany, CompanyResponseDTO.class);
    }

    @Override
    public void delete(UUID id, PublicUserDTO user) {
        Company company = getEntityByIdAdmin(id);

        if (!user.getRole().equals(Role.ADMIN)) {
            if (!company.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException();
            }
        }

        if (company.getDeletedAt() == null) {
            company.delete();
        } else {
            company.setDeletedAt(null);
        }

        companyRepository.save(company);
    }

    @Override
    public Company getEntityById(UUID id) {
        Optional<Company> company = companyRepository.findByIdAndDeletedAtIsNull(id);

        if (company.isEmpty()) {
            throw new CompanyNotFoundException();
        }

        return company.get();
    }

    @Override
    public Company getEntityByIdAdmin(UUID id) {
        Optional<Company> company = companyRepository.findById(id);

        if (company.isEmpty()) {
            throw new CompanyNotFoundException();
        }

        return company.get();
    }

    public void mapRequestDTOIdsToEntities(CompanyRequestDTO companyRequestDTO, Company company) {
        if (companyRequestDTO.getCityId() != null) {
            City city = cityService.getEntityById(companyRequestDTO.getCityId());
            company.setCity(city);
        }

        if (companyRequestDTO.getMainPictureId() != null) {
            File mainPicture = fileService.getEntityById(companyRequestDTO.getMainPictureId());
            company.setMainPicture(mainPicture);
        }

        if (companyRequestDTO.getPictureIds() != null) {
            Set<File> pictures = companyRequestDTO.getPictureIds().stream().map(fileService::getEntityById).collect(Collectors.toSet());
            company.setPictures(pictures);
        }
    }

    private Sort getCompanySort(CompanySort companySort) {
        if (companySort.equals(CompanySort.def)) {
            return Sort.by("createdAt").descending();
        } else if (companySort.equals(CompanySort.alphaDesc)) {
            return Sort.by("name").ascending();
        }

        throw new BadRequestException("Невалидно сортиране!");
    }
}
