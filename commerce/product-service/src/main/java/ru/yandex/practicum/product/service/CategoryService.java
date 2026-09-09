package ru.yandex.practicum.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.CategoryMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(Long id) {
        return categoryMapper.toDto(findById(id));
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id " + id + " не найдена"
                ));
    }
}