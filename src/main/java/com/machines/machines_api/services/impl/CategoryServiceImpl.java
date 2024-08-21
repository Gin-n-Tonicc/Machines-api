package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.category.CategoryCreateException;
import com.machines.machines_api.exceptions.category.CategoryNotFoundException;
import com.machines.machines_api.models.dto.request.CategoryRequestDTO;
import com.machines.machines_api.models.dto.response.CategoryResponseDTO;
import com.machines.machines_api.models.dto.response.admin.CategoryAdminResponseDTO;
import com.machines.machines_api.models.entity.Category;
import com.machines.machines_api.repositories.CategoryRepository;
import com.machines.machines_api.services.CategoryService;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CategoryResponseDTO> getAll() {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNull();
        return categories.stream().map(x -> modelMapper.map(x, CategoryResponseDTO.class)).toList();
    }

    @Override
    public List<CategoryAdminResponseDTO> getAllAdmin() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(x -> modelMapper.map(x, CategoryAdminResponseDTO.class)).toList();
    }

    @Override
    public CategoryResponseDTO getCategoryById(UUID id) {
        Category category = getCategoryEntityById(id);
        return modelMapper.map(category, CategoryResponseDTO.class);
    }

    @Override
    public CategoryAdminResponseDTO getCategoryByIdAdmin(UUID id) {
        Category category = getCategoryEntityByIdAdmin(id);
        return modelMapper.map(category, CategoryAdminResponseDTO.class);
    }

    @Override
    public CategoryResponseDTO create(CategoryRequestDTO categoryDTO) {
        // Make sure category is unique
        if (categoryRepository.findByNameAndDeletedAtIsNull(categoryDTO.getName()).isPresent()) {
            throw new CategoryCreateException(true);
        }

        // Persist category
        try {
            Category category = categoryRepository.save(modelMapper.map(categoryDTO, Category.class));
            category.setSubcategories(new ArrayList<>());

            return modelMapper.map(category, CategoryResponseDTO.class);
        } catch (RuntimeException exception) {
            throw new CategoryCreateException(false);
        }
    }

    @Override
    public CategoryResponseDTO update(UUID id, CategoryRequestDTO categoryDTO) {
        Category existingCategory = getCategoryEntityByIdAdmin(id);
        Optional<Category> potentialCategory = categoryRepository.findByNameAndDeletedAtIsNull(categoryDTO.getName());

        if (potentialCategory.isPresent() && !existingCategory.getId().equals(potentialCategory.get().getId())) {
            throw new CategoryCreateException(true);
        }

        modelMapper.map(categoryDTO, existingCategory);
        existingCategory.setId(id);

        Category updatedCategory = categoryRepository.save(existingCategory);
        return modelMapper.map(updatedCategory, CategoryResponseDTO.class);
    }

    @Override
    public void delete(UUID id) {
        Category existingCategory = getCategoryEntityByIdAdmin(id);

        if (existingCategory.getDeletedAt() == null) {
            existingCategory.setDeletedAt(LocalDateTime.now());
        } else {
            existingCategory.setDeletedAt(null);
        }

        categoryRepository.save(existingCategory);
    }

    public Category getCategoryEntityById(UUID id) {
        Optional<Category> category = categoryRepository.findByIdAndDeletedAtIsNull(id);

        if (category.isEmpty()) {
            throw new CategoryNotFoundException();
        }

        return category.get();
    }

    public Category getCategoryEntityByIdAdmin(UUID id) {
        Optional<Category> category = categoryRepository.findById(id);

        if (category.isEmpty()) {
            throw new CategoryNotFoundException();
        }

        return category.get();
    }
}
