// Main JavaScript functionality
document.addEventListener('DOMContentLoaded', function() {
    // Auto-dismiss alerts after 5 seconds (except URL result alerts)
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        // Don't auto-dismiss alerts that contain URL results or have persistent-result class
        if (!alert.querySelector('#shortenedUrl') && 
            !alert.querySelector('#persistentShortenedUrl') && 
            !alert.classList.contains('persistent-result') &&
            !alert.innerHTML.includes('shortened URL') &&
            !alert.innerHTML.includes('Your shortened URL') &&
            !alert.innerHTML.includes('URL shortened successfully') &&
            !alert.innerHTML.includes('Success!')) {
            setTimeout(() => {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            }, 5000);
        }
    });

    // Add fade-in animation to cards
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        card.style.opacity = '0';
        setTimeout(() => {
            card.classList.add('fade-in');
            card.style.opacity = '1';
        }, index * 100);
    });

    // Password strength indicator
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            const strength = checkPasswordStrength(this.value);
            updatePasswordStrengthIndicator(strength);
        });
    }

    // Confirm dialog for dangerous actions
    const dangerButtons = document.querySelectorAll('[data-confirm]');
    dangerButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const message = this.getAttribute('data-confirm');
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });

    // Copy to clipboard functionality
    const copyButtons = document.querySelectorAll('[data-copy]');
    copyButtons.forEach(button => {
        button.addEventListener('click', function() {
            const textToCopy = this.getAttribute('data-copy');
            navigator.clipboard.writeText(textToCopy).then(() => {
                showToast('Copied to clipboard!');
            });
        });
    });
});

// Password strength checker
function checkPasswordStrength(password) {
    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.match(/[a-z]/)) strength++;
    if (password.match(/[A-Z]/)) strength++;
    if (password.match(/[0-9]/)) strength++;
    if (password.match(/[^a-zA-Z0-9]/)) strength++;
    return strength;
}

// Update password strength indicator
function updatePasswordStrengthIndicator(strength) {
    const indicator = document.getElementById('password-strength');
    if (!indicator) return;

    const strengthTexts = ['', 'Very Weak', 'Weak', 'Fair', 'Good', 'Strong'];
    const strengthColors = ['', '#f44336', '#ff9800', '#ffc107', '#8bc34a', '#4caf50'];

    indicator.textContent = strengthTexts[strength];
    indicator.style.color = strengthColors[strength];
}

// Show toast notification
function showToast(message) {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        const container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999;';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'alert alert-success alert-dismissible fade show';
    toast.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.getElementById('toast-container').appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 3000);
}