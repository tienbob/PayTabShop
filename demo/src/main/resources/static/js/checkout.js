// static/js/checkout.js
$(document).ready(function() {
    $('#checkout-form').submit(function(event) {
        event.preventDefault();

        const customerName = $('#customer_name').val();
        const customerEmail = $('#customer_email').val();
        const shippingAddress = $('#shipping_address').val();
        const paymentOption = $('#payment_option').val();
        const productId = $('#productId').val();
        const quantity = $('#quantity').val();
        const formData = {
            customerName: customerName,
            customerEmail: customerEmail,
            shippingAddress: shippingAddress,
            paymentOption: paymentOption,
            productId: productId,
            quantity: quantity
        };

        $.ajax({
            type: 'POST',
            url: '/checkout',
            data: formData,
            success: function(data) {
                if (data) {
                    // Create a hidden form and submit the payment to PayTabs
                    const form = $('<form>', {
                        'method': 'POST',
                        'action': 'https://secure.paytabs.com/payment/request'
                    });

                    for (const key in data) {
                        if (data.hasOwnProperty(key)) {
                            const input = $('<input>', {
                                'type': 'hidden',
                                'name': key,
                                'value': data[key]
                            });
                            form.append(input);
                        }
                    }

                    $('#payment-form-container').append(form);
                    form.submit();
                } else {
                    alert('Error: Could not retrieve payment request payload.');
                }
            },
            error: function(error) {
                console.error('Error:', error);
                alert('Error: ' + error.responseText); // Display server-side error messages
            }
        });
    });
});