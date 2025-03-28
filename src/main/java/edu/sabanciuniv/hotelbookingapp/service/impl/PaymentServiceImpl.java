package edu.sabanciuniv.hotelbookingapp.service.impl;

import edu.sabanciuniv.hotelbookingapp.model.Booking;
import edu.sabanciuniv.hotelbookingapp.model.Payment;
import edu.sabanciuniv.hotelbookingapp.model.dto.BookingInitiationDTO;
import edu.sabanciuniv.hotelbookingapp.model.enums.Currency;
import edu.sabanciuniv.hotelbookingapp.model.enums.PaymentMethod;
import edu.sabanciuniv.hotelbookingapp.model.enums.PaymentStatus;
import edu.sabanciuniv.hotelbookingapp.repository.PaymentRepository;
import edu.sabanciuniv.hotelbookingapp.service.PaymentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Refund;
import com.paypal.api.payments.Sale;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
	
	@Value("${paypal.client.id}")
	private String clientId;

	@Value("${paypal.client.secret}")
	private String clientSecret;

	@Value("${paypal.mode}")
	private String mode;

	private APIContext apiContext;
	
	@PostConstruct
	public void init() throws PayPalRESTException {
		Map<String, String> config = new HashMap<>();
		config.put("mode", mode);
		apiContext = new APIContext(new OAuthTokenCredential(clientId, clientSecret, config).getAccessToken());
		apiContext.setConfigurationMap(config);
	}
	
	public APIContext getAPIContext() {
		return apiContext;
	}

    private final PaymentRepository paymentRepository;

    @Override
    public Payment savePayment(BookingInitiationDTO bookingInitiationDTO, Booking booking, String transactionId) {
        
    	Payment payment = Payment.builder()
                .booking(booking)
                .totalPrice(bookingInitiationDTO.getTotalPrice())
                .paymentStatus(PaymentStatus.COMPLETED) // Assuming the payment is completed
                .paymentMethod(PaymentMethod.PAYPAL) // Default to Paypal
                .currency(Currency.USD) // Default to USD
                .build();
    	
    	payment.setTransactionId(transactionId);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved with transaction ID: {}", savedPayment.getTransactionId());

        return savedPayment;
    }
    
    @Override
    public com.paypal.api.payments.Payment excecutePayment(String paymentId, String payerId) throws PayPalRESTException {
		com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
		payment.setId(paymentId);
		PaymentExecution paymentExecute = new PaymentExecution();
		paymentExecute.setPayerId(payerId);
		return payment.execute(apiContext, paymentExecute);
	}
    
    @Override
    public com.paypal.api.payments.Payment createPayment(Double total,
			String currency,
			String Method,
			String intent,
			String description,
			String cancelUrl,
			String successUrl) {
    	
    	Amount amount = new Amount();
		amount.setCurrency(currency);
		amount.setTotal(String.format(Locale.forLanguageTag(currency), "%.2f", total));

		Transaction transaction = new Transaction();
		transaction.setDescription(description);
		transaction.setAmount(amount);

		List<Transaction> transactions = new ArrayList<>();
		transactions.add(transaction);

		Payer payer = new Payer();
		payer.setPaymentMethod(Method);

		com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
		payment.setIntent(intent);
		payment.setPayer(payer);
		payment.setTransactions(Arrays.asList(transaction));
		RedirectUrls redirectUrls = new RedirectUrls();
		redirectUrls.setCancelUrl(cancelUrl);
		redirectUrls.setReturnUrl(successUrl);
		payment.setRedirectUrls(redirectUrls);

		try {
			return payment.create(apiContext);
		} catch (PayPalRESTException e) {
			e.printStackTrace();
		}
		
		return null;	
    }
    
    public Refund refundPayment(String saleId, Double refundAmount) throws PayPalRESTException {
        // Create the Refund object to specify the refund amount
        Refund refund = new Refund();
        Amount amount = new Amount();
        amount.setTotal(String.format("%.2f", refundAmount));
        amount.setCurrency("USD"); // Use the same currency as the original payment
        refund.setAmount(amount);

        // Create the Sale object to interact with PayPal's Sale resource
        Sale sale = new Sale();
        sale.setId(saleId); // Set the Sale ID from the payment you want to refund

        // Execute the refund request
        try {
            // Call the refund method on the Sale object
            Refund refundedSale = sale.refund(apiContext, refund); 
            log.info("Refund successful. Sale ID: {}", refundedSale.getId());
            return refundedSale;
        } catch (PayPalRESTException e) {
            log.error("Error during refund. Error message: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void refundPayment(String transactionId) throws PayPalRESTException {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for transaction ID: " + transactionId));

        Sale sale = new Sale();
        sale.setId(transactionId);

        Refund refund = new Refund();
        Amount amount = new Amount();
        amount.setCurrency(payment.getCurrency().name()); // Use the currency from the payment
        amount.setTotal(payment.getTotalPrice().toString()); // Use the total price from the payment
        refund.setAmount(amount);

        Refund refundedSale = sale.refund(apiContext, refund);
        log.info("Refund successful. Refund ID: {}", refundedSale.getId());
    }
}
