package com.machines.machines_api.services.impl;

import com.machines.machines_api.models.dto.request.PaymentRequestDTO;
import com.machines.machines_api.models.entity.Payment;
import com.machines.machines_api.repositories.PaymentRepository;
import com.machines.machines_api.services.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    @Override
    public void save(PaymentRequestDTO paymentRequestDTO) {
        Payment payment = modelMapper.map(paymentRequestDTO, Payment.class);
        paymentRepository.save(payment);
    }

    @Override
    public PaymentMethod getPaymentMethod(String paymentMethod) {
        try {
            return PaymentMethod.retrieve(paymentMethod);
        } catch (StripeException e) {
            return null;
        }
    }
}
