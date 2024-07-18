package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.category.CategoryCreateException;
import com.machines.machines_api.exceptions.category.CategoryNotFoundException;
import com.machines.machines_api.models.dto.request.CategoryRequestDTO;
import com.machines.machines_api.models.dto.response.CategoryResponseDTO;
import com.machines.machines_api.models.entity.Category;
import com.machines.machines_api.repositories.CategoryRepository;
import com.machines.machines_api.services.CategoryService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final MessageSource messageSource;
    private final ModelMapper modelMapper;

    @Override
    public List<CategoryResponseDTO> getAll() {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNull();
        return categories.stream().map(x -> modelMapper.map(x, CategoryResponseDTO.class)).toList();
    }

    @Override
    public CategoryResponseDTO getCategoryById(UUID id) {
        Category category = getCategoryEntityById(id);
        return modelMapper.map(category, CategoryResponseDTO.class);
    }

    @Override
    public CategoryResponseDTO create(CategoryRequestDTO categoryDTO) {
        // Make sure category is unique
        if (categoryRepository.findByNameAndDeletedAtIsNull(categoryDTO.getName()).isPresent()) {
            throw new CategoryCreateException(messageSource, true);
        }

        // Persist category
        try {
            Category category = categoryRepository.save(modelMapper.map(categoryDTO, Category.class));
            return modelMapper.map(category, CategoryResponseDTO.class);
        } catch (RuntimeException exception) {
            throw new CategoryCreateException(messageSource, false);
        }
    }

    @Override
    public CategoryResponseDTO update(UUID id, CategoryRequestDTO categoryDTO) {
        Category existingCategory = getCategoryEntityById(id);

        modelMapper.map(categoryDTO, existingCategory);
        existingCategory.setId(id);

        Category updatedCategory = categoryRepository.save(existingCategory);
        return modelMapper.map(updatedCategory, CategoryResponseDTO.class);
    }

    @Override
    public void delete(UUID id) {
        Category existingCategory = getCategoryEntityById(id);
        existingCategory.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(existingCategory);
    }

    public Category getCategoryEntityById(UUID id) {
        Optional<Category> category = categoryRepository.findByIdAndDeletedAtIsNull(id);

        if (category.isEmpty()) {
            throw new CategoryNotFoundException(messageSource);
        }

        return category.get();
    }
}