package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.category.CategoryCreateException;
import com.machines.machines_api.exceptions.category.CategoryNotFoundException;
import com.machines.machines_api.models.dto.request.SubcategoryRequestDTO;
import com.machines.machines_api.models.dto.response.SubcategoryResponseDTO;
import com.machines.machines_api.models.dto.response.admin.SubcategoryAdminResponseDTO;
import com.machines.machines_api.models.entity.Category;
import com.machines.machines_api.models.entity.Subcategory;
import com.machines.machines_api.repositories.SubcategoryRepository;
import com.machines.machines_api.services.CategoryService;
import com.machines.machines_api.services.SubcategoryService;
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
public class SubcategoryServiceImpl implements SubcategoryService {
    private final CategoryService categoryService;
    private final SubcategoryRepository subcategoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<SubcategoryResponseDTO> getAll() {
        List<Subcategory> subcategories = subcategoryRepository.findAllByDeletedAtIsNull();
        return subcategories.stream().map(x -> modelMapper.map(x, SubcategoryResponseDTO.class)).toList();
    }

    @Override
    public List<SubcategoryAdminResponseDTO> getAllAdmin() {
        List<Subcategory> subcategories = subcategoryRepository.findAll();
        return subcategories.stream().map(x -> modelMapper.map(x, SubcategoryAdminResponseDTO.class)).toList();
    }

    @Override
    public SubcategoryResponseDTO getById(UUID id) {
        Subcategory subcategory = getSubCategoryEntityById(id);
        return modelMapper.map(subcategory, SubcategoryResponseDTO.class);
    }

    @Override
    public SubcategoryAdminResponseDTO getByIdAdmin(UUID id) {
        Subcategory subcategory = getSubCategoryEntityByIdAdmin(id);
        return modelMapper.map(subcategory, SubcategoryAdminResponseDTO.class);
    }

    @Override
    public SubcategoryResponseDTO create(SubcategoryRequestDTO subcategoryDTO) {
        // Make sure subcategory is unique
        if (subcategoryRepository.findByNameAndDeletedAtIsNull(subcategoryDTO.getName()).isPresent()) {
            throw new CategoryCreateException(true);
        }

        Category mainCategory = categoryService.getCategoryEntityById(subcategoryDTO.getCategoryId());
        Subcategory subcategory = modelMapper.map(subcategoryDTO, Subcategory.class);
        subcategory.setCategory(mainCategory);
        subcategory.setId(null);

        // Persist subcategory
        try {
            Subcategory savedSubcategory = subcategoryRepository.save(subcategory);
            return modelMapper.map(savedSubcategory, SubcategoryResponseDTO.class);
        } catch (RuntimeException exception) {
            throw new CategoryCreateException(false);
        }
    }

    @Override
    public SubcategoryResponseDTO update(UUID id, SubcategoryRequestDTO subcategoryDTO) {
        Subcategory subcategory = getSubCategoryEntityByIdAdmin(id);
        Optional<Subcategory> potentialSubcategory = subcategoryRepository.findByNameAndDeletedAtIsNull(subcategoryDTO.getName());

        if (potentialSubcategory.isPresent() && !subcategory.getId().equals(potentialSubcategory.get().getId())) {
            throw new CategoryCreateException(true);
        }

        if (subcategoryDTO.getName() != null) {
            subcategory.setName(subcategoryDTO.getName());
        }

        if (subcategoryDTO.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntityById(subcategoryDTO.getCategoryId());
            subcategory.setCategory(category);
        }

        Subcategory updatedSubcategory = subcategoryRepository.save(subcategory);
        return modelMapper.map(updatedSubcategory, SubcategoryResponseDTO.class);
    }

    @Override
    public void delete(UUID id) {
        Subcategory subcategory = getSubCategoryEntityByIdAdmin(id);

        if (subcategory.getDeletedAt() == null) {
            subcategory.setDeletedAt(LocalDateTime.now());
        } else {
            subcategory.setDeletedAt(null);
        }

        subcategoryRepository.save(subcategory);
    }

    @Override
    public Subcategory getSubCategoryEntityById(UUID id) {
        Optional<Subcategory> subcategory = subcategoryRepository.findByIdAndDeletedAtIsNull(id);

        if (subcategory.isEmpty()) {
            throw new CategoryNotFoundException();
        }

        return subcategory.get();
    }

    public Subcategory getSubCategoryEntityByIdAdmin(UUID id) {
        Optional<Subcategory> subcategory = subcategoryRepository.findById(id);

        if (subcategory.isEmpty()) {
            throw new CategoryNotFoundException();
        }

        return subcategory.get();
    }
}
