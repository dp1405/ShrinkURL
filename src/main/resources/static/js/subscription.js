// Subscription specific JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Update plan comparison
    const planCards = document.querySelectorAll('.plan-card');
    planCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.classList.add('shadow-lg');
        });
        
        card.addEventListener('mouseleave', function() {
            this.classList.remove('shadow-lg');
        });
    });

    // Plan selection confirmation
    const upgradeForms = document.querySelectorAll('form[action*="/subscription/upgrade"]');
    upgradeForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const planName = this.querySelector('input[name="plan"]').value;
            const message = `Are you sure you want to upgrade to the ${planName} plan?`;
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });

    // Cancel subscription confirmation
    const cancelForm = document.getElementById('cancel-subscription-form');
    if (cancelForm) {
        cancelForm.addEventListener('submit', function(e) {
            const confirmed = confirm('Are you sure you want to cancel your subscription? You will retain access until the end of your billing period.');
            if (!confirmed) {
                e.preventDefault();
            }
        });
    }

    // Payment simulation
    const paymentSimulateForm = document.getElementById('payment-simulate-form');
    if (paymentSimulateForm) {
        paymentSimulateForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Show loading state
            const submitButton = this.querySelector('button[type="submit"]');
            const originalText = submitButton.innerHTML;
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
            submitButton.disabled = true;
            
            // Simulate payment processing
            setTimeout(() => {
                const status = document.querySelector('input[name="payment-status"]:checked').value;
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = '/subscription/payment/callback';
                
                const fields = {
                    'subscriptionId': document.getElementById('subscriptionId').value,
                    'status': status,
                    'paymentId': 'PAY_' + Date.now(),
                    'transactionId': 'TXN_' + Date.now()
                };
                
                for (const [key, value] of Object.entries(fields)) {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = key;
                    input.value = value;
                    form.appendChild(input);
                }
                
                document.body.appendChild(form);
                form.submit();
            }, 2000);
        });
    }

    // Usage progress animation
    const progressBars = document.querySelectorAll('.progress-bar');
    progressBars.forEach(bar => {
        const width = bar.style.width;
        bar.style.width = '0%';
        setTimeout(() => {
            bar.style.transition = 'width 1s ease-out';
            bar.style.width = width;
        }, 100);
    });
});

// Calculate remaining days percentage
function calculateRemainingPercentage(endDate) {
    const now = new Date();
    const end = new Date(endDate);
    const total = end - now;
    const days = Math.ceil(total / (1000 * 60 * 60 * 24));
    return days > 0 ? days : 0;
}