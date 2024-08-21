package com.machines.machines_api.services.impl;

import com.machines.machines_api.config.StripeConfig;
import com.machines.machines_api.exceptions.payment.CheckoutCreateException;
import com.machines.machines_api.models.dto.metadata.OfferMetadata;
import com.machines.machines_api.models.dto.request.checkout.HostedCheckoutRequestDTO;
import com.machines.machines_api.models.dto.request.checkout.OfferCheckoutRequestDTO;
import com.machines.machines_api.models.entity.Product;
import com.machines.machines_api.services.CheckoutService;
import com.machines.machines_api.services.ProductService;
import com.machines.machines_api.utils.CustomerUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Component
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {
    private final ProductService productService;
    private final StripeConfig stripeConfig;

    @Override
    public String createHostedCheckoutSession(HostedCheckoutRequestDTO checkoutRequestDTO) throws StripeException {
        if (checkoutRequestDTO.getCheckoutIdsMap().isEmpty()) {
            throw new CheckoutCreateException();
        }

        Customer customer = CustomerUtil.findOrCreateCustomer(checkoutRequestDTO.getCustomerEmail(), checkoutRequestDTO.getCustomerName());

        SessionCreateParams.Builder paramsBuilder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setCustomer(customer.getId())
                        .setSuccessUrl(stripeConfig.getSuccessUrl())
                        .setCancelUrl(stripeConfig.getCancelUrl());

        Map<String, Map<String, String>> checkoutIdsMap = checkoutRequestDTO.getCheckoutIdsMap();
        List<Product> products = checkoutIdsMap.keySet().stream().map(productService::getByCheckoutId).toList();

        for (Product product : products) {
            Map<String, String> metadata = checkoutIdsMap.get(product.getCheckoutId());
            metadata.put("app_id", product.getCheckoutId());

            paramsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .putAllMetadata(metadata)
                                                            .setName(product.getName())
                                                            .build()
                                            )
                                            .setCurrency(product.getCurrency())
                                            .setUnitAmountDecimal(product.getUnitAmountDecimalInCents())
                                            .build())
                            .build());
        }

        Session session = Session.create(paramsBuilder.build());

        return session.getUrl();
    }

    @Override
    public String createPromoteOfferHostedCheckoutSession(OfferCheckoutRequestDTO offerCheckoutRequestDTO) throws StripeException {
        Map<String, Map<String, String>> checkoutIds = new HashMap<>();

        OfferMetadata offerMetadata = new OfferMetadata(
                offerCheckoutRequestDTO.getOfferId(),
                offerCheckoutRequestDTO.getOfferType()
        );

        checkoutIds.put(offerCheckoutRequestDTO.getOfferType().getCheckoutId(), offerMetadata);

        HostedCheckoutRequestDTO checkoutRequestDTO = new HostedCheckoutRequestDTO(checkoutIds, offerCheckoutRequestDTO);
        return createHostedCheckoutSession(checkoutRequestDTO);
    }
}
