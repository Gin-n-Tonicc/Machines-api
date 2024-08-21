package com.machines.machines_api.services.impl;

import com.machines.machines_api.exceptions.product.ProductExistsException;
import com.machines.machines_api.interfaces.CheckoutProduct;
import com.machines.machines_api.models.entity.Product;
import com.machines.machines_api.repositories.ProductRepository;
import com.machines.machines_api.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Component
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public Product getByCheckoutId(String checkoutId) {
        Optional<Product> product = productRepository.findByCheckoutIdAndDeletedAtIsNull(checkoutId);

        if (product.isEmpty()) {
            throw new ProductExistsException("checkoutId");
        }

        return product.get();
    }

    @Override
    public void saveAllIfNotAdded(List<CheckoutProduct> checkoutProducts) {
        List<Product> products = new ArrayList<>();

        for (CheckoutProduct checkoutProduct : checkoutProducts) {
            Optional<Product> product = productRepository.findByCheckoutIdAndDeletedAtIsNull(checkoutProduct.getCheckoutId());

            if (product.isEmpty()) {
                products.add(toProduct(checkoutProduct));
            }
        }

        productRepository.saveAll(products);
    }

    private Product toProduct(CheckoutProduct checkoutProduct) {
        Product product = new Product();

        product.setName(checkoutProduct.getName());
        product.setCheckoutId(checkoutProduct.getCheckoutId());
        product.setCurrency(checkoutProduct.getCurrency());
        product.setUnitAmountDecimal(checkoutProduct.getUnitAmountDecimal());

        return product;
    }
}
