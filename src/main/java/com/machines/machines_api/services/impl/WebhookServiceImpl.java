package com.machines.machines_api.services.impl;

import com.google.gson.JsonSyntaxException;
import com.machines.machines_api.config.StripeConfig;
import com.machines.machines_api.enums.PaymentProvider;
import com.machines.machines_api.exceptions.common.BadRequestException;
import com.machines.machines_api.exceptions.common.InternalServerErrorException;
import com.machines.machines_api.models.dto.common.PaymentDTO;
import com.machines.machines_api.models.dto.metadata.OfferMetadata;
import com.machines.machines_api.models.dto.request.PaymentRequestDTO;
import com.machines.machines_api.services.OfferService;
import com.machines.machines_api.services.PaymentService;
import com.machines.machines_api.services.WebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {
    private final StripeConfig stripeConfig;
    private final OfferService offerService;
    private final PaymentService paymentService;
    private final ModelMapper modelMapper;

    @Override
    public Event constructEvent(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (JsonSyntaxException e) {
            throw new BadRequestException("Failed payload verification");
        } catch (SignatureVerificationException e) {
            throw new BadRequestException("Failed signature verification");
        }

        return event;
    }

    @Override
    public StripeObject convertStripeObject(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;

        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            throw new IllegalStateException(
                    String.format("Unable to deserialize event data object for %s", event));
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
            // instructions on how to handle this case, or return an error here.
        }

        return stripeObject;
    }

    @Override
    public void handleStripeEvent(Event event, StripeObject stripeObject) {
        if (event.getType().equals("payment_intent.succeeded")) {
            PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
            handlePaymentSucceeded(event, paymentIntent);
            return;
        }

        throw new InternalServerErrorException("Unexpected event type");
    }

    public void handlePaymentSucceeded(Event event, PaymentIntent paymentIntent) {
        var metadata = paymentIntent.getMetadata();

        PaymentRequestDTO paymentRequestDTO = getPaymentRequestDTO(paymentIntent);
        paymentService.save(paymentRequestDTO);

        if (OfferMetadata.isOfferMetadata(metadata)) {
            handleOfferPromotePaymentSucceeded(paymentIntent);
        }
    }

    public void handleOfferPromotePaymentSucceeded(PaymentIntent paymentIntent) {
        OfferMetadata metadata = (OfferMetadata) paymentIntent.getMetadata();
        offerService.updateOfferType(metadata.getOfferId(), metadata.getOfferType());
    }

    private PaymentRequestDTO getPaymentRequestDTO(PaymentIntent paymentIntent) {
        var paymentDTOBuilder = PaymentDTO.builder()
                .status(paymentIntent.getStatus())
                .description(paymentIntent.getDescription())
                .amount(paymentIntent.getAmount())
                .amountReceived(paymentIntent.getAmountReceived())
                .currency(paymentIntent.getCurrency())
                .metadata(paymentIntent.getMetadata().toString())
                .paymentProvider(PaymentProvider.STRIPE);

        Customer customer = paymentIntent.getCustomerObject();
        if (customer != null) {
            paymentDTOBuilder
                    .customerId(customer.getId())
                    .customerEmail(customer.getEmail())
                    .customerName(customer.getName());
        }

        PaymentMethod paymentMethod = paymentService.getPaymentMethod(paymentIntent.getPaymentMethod());
        if (paymentMethod != null) {
            paymentDTOBuilder.paymentMethodType(paymentMethod.getType());
        }

        PaymentDTO paymentDTO = paymentDTOBuilder.build();
        return modelMapper.map(paymentDTO, PaymentRequestDTO.class);
    }
}
