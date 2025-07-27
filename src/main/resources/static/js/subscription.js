// Enhanced Subscription JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Plan card hover effects
    const planCards = document.querySelectorAll('.plan-card');
    planCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.classList.add('shadow-lg');
        });
        
        card.addEventListener('mouseleave', function() {
            this.classList.remove('shadow-lg');
        });
    });

    // Enhanced plan selection with animation
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

    // Enhanced payment simulation
    const paymentSimulateForm = document.getElementById('payment-simulate-form');
    if (paymentSimulateForm) {
        paymentSimulateForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const submitButton = this.querySelector('button[type="submit"]');
            const originalText = submitButton.innerHTML;
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing Payment...';
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
                    'paymentId': 'pay_' + Math.random().toString(36).substr(2, 9),
                    'transactionId': 'txn_' + Math.random().toString(36).substr(2, 9)
                };
                
                Object.entries(fields).forEach(([key, value]) => {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = key;
                    input.value = value;
                    form.appendChild(input);
                });
                
                document.body.appendChild(form);
                form.submit();
            }, 3000);
        });
    }
    
    // Usage statistics animations
    animateUsageProgress();
    
    // Plan comparison tooltips
    initializePlanTooltips();
});

function animateUsageProgress() {
    const progressBars = document.querySelectorAll('.usage-progress .progress-bar');
    
    progressBars.forEach(bar => {
        const width = bar.style.width;
        bar.style.width = '0%';
        
        setTimeout(() => {
            bar.style.transition = 'width 1s ease-in-out';
            bar.style.width = width;
        }, 500);
    });
}

function initializePlanTooltips() {
    const planCards = document.querySelectorAll('.plan-card');
    
    planCards.forEach(card => {
        const features = card.querySelectorAll('.plan-features li');
        
        features.forEach(feature => {
            const text = feature.textContent.trim();
            
            // Add tooltips for specific features
            if (text.includes('Custom short codes')) {
                feature.setAttribute('title', 'Create branded short links with custom aliases');
            } else if (text.includes('Custom domains')) {
                feature.setAttribute('title', 'Use your own domain for short links');
            } else if (text.includes('Advanced analytics')) {
                feature.setAttribute('title', 'Detailed click tracking, geographic data, and more');
            } else if (text.includes('Priority support')) {
                feature.setAttribute('title', 'Get faster response times and dedicated support');
            }
        });
    });
}

// Calculate remaining days percentage
function calculateRemainingPercentage(endDate) {
    const now = new Date();
    const end = new Date(endDate);
    const total = end - now;
    const days = Math.ceil(total / (1000 * 60 * 60 * 24));
    return days > 0 ? days : 0;
}