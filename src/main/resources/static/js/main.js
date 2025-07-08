// JWT Token Management
const JWTManager = {
    tokenKey: 'shrinkurl_jwt_token',
    refreshTokenKey: 'shrinkurl_refresh_token',
    
    getToken: function() {
        return localStorage.getItem(this.tokenKey);
    },
    
    setToken: function(token) {
        localStorage.setItem(this.tokenKey, token);
    },
    
    getRefreshToken: function() {
        return localStorage.getItem(this.refreshTokenKey);
    },
    
    setRefreshToken: function(refreshToken) {
        localStorage.setItem(this.refreshTokenKey, refreshToken);
    },
    
    removeTokens: function() {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.refreshTokenKey);
    },
    
    isTokenExpired: function(token) {
        if (!token) return true;
        
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const currentTime = Date.now() / 1000;
            return payload.exp < currentTime;
        } catch (e) {
            return true;
        }
    },
    
    refreshToken: function() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            return Promise.reject('No refresh token available');
        }
        
        return fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken: refreshToken })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Token refresh failed');
            }
            return response.json();
        })
        .then(data => {
            this.setToken(data.accessToken);
            if (data.refreshToken) {
                this.setRefreshToken(data.refreshToken);
            }
            return data.accessToken;
        })
        .catch(error => {
            this.removeTokens();
            // Redirect to login if refresh fails
            window.location.href = '/auth/login';
            throw error;
        });
    }
};

// Global application object
const ShrinkURL = {
    init: function() {
        this.bindEvents();
        this.initTooltips();
        this.initScrollEffects();
        this.checkTokenExpiry();
    },

    checkTokenExpiry: function() {
        const token = JWTManager.getToken();
        if (token && JWTManager.isTokenExpired(token)) {
            JWTManager.refreshToken().catch(() => {
                // Token refresh failed, user will be redirected to login
            });
        }
    },

    bindEvents: function() {
        // Copy button functionality
        document.addEventListener('click', function(e) {
            if (e.target.matches('#copyBtn, #copyBtn *')) {
                ShrinkURL.copyToClipboard();
            }
        });

        // Smooth scrolling for anchor links
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function(e) {
                e.preventDefault();
                const target = document.querySelector(this.getAttribute('href'));
                if (target) {
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });

        // Auto-hide alerts
        setTimeout(() => {
            const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
            alerts.forEach(alert => {
                alert.style.transition = 'opacity 0.5s ease';
                alert.style.opacity = '0';
                setTimeout(() => alert.remove(), 500);
            });
        }, 5000);
    },

    copyToClipboard: function() {
        const shortenedUrl = document.getElementById('shortenedUrl');
        const copyBtn = document.getElementById('copyBtn');
        
        if (shortenedUrl) {
            shortenedUrl.select();
            shortenedUrl.setSelectionRange(0, 99999);
            
            try {
                document.execCommand('copy');
                const originalText = copyBtn.innerHTML;
                copyBtn.innerHTML = '<i class="fas fa-check"></i> Copied!';
                copyBtn.classList.add('btn-success');
                copyBtn.classList.remove('btn-outline-primary');
                
                setTimeout(() => {
                    copyBtn.innerHTML = originalText;
                    copyBtn.classList.remove('btn-success');
                    copyBtn.classList.add('btn-outline-primary');
                }, 2000);
                
                this.showToast('URL copied to clipboard!', 'success');
            } catch (err) {
                this.showToast('Failed to copy URL. Please try again.', 'error');
            }
        }
    },

    showToast: function(message, type = 'info') {
        const toastContainer = document.querySelector('.toast-container') || this.createToastContainer();
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : type} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-${type === 'success' ? 'check' : type === 'error' ? 'exclamation-triangle' : 'info'}"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        toastContainer.appendChild(toast);
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        // Remove toast after it's hidden
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    },

        createToastContainer: function() {
        const container = document.createElement('div');
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1050';
        document.body.appendChild(container);
        return container;
    },

    initTooltips: function() {
        // Initialize Bootstrap tooltips
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function(tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    },

    initScrollEffects: function() {
        // Add scroll-based animations
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver(function(entries) {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('fade-in');
                }
            });
        }, observerOptions);

        // Observe elements for animation
        document.querySelectorAll('.feature-card, .stat-item').forEach(el => {
            observer.observe(el);
        });
    },

    // Utility functions
    formatNumber: function(num) {
        if (num >= 1000000) {
            return (num / 1000000).toFixed(1) + 'M';
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + 'K';
        }
        return num.toString();
    },

    debounce: function(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    // API helper functions
    makeRequest: function(url, options = {}) {
        const defaultOptions = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        };

        const mergedOptions = { ...defaultOptions, ...options };
        
        // Add JWT token if available
        const token = JWTManager.getToken();
        if (token && !JWTManager.isTokenExpired(token)) {
            mergedOptions.headers['Authorization'] = `Bearer ${token}`;
        }

        return fetch(url, mergedOptions)
            .then(response => {
                if (response.status === 401) {
                    // Token expired, try to refresh
                    return JWTManager.refreshToken()
                        .then(newToken => {
                            // Retry the request with new token
                            mergedOptions.headers['Authorization'] = `Bearer ${newToken}`;
                            return fetch(url, mergedOptions);
                        });
                }
                return response;
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .catch(error => {
                console.error('Request failed:', error);
                throw error;
            });
    }
};

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    ShrinkURL.init();
});

// Add loading states to forms
document.addEventListener('submit', function(e) {
    const form = e.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    
    if (submitBtn && !submitBtn.disabled) {
        submitBtn.disabled = true;
        submitBtn.classList.add('loading');
        
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<span class="spinner"></span> Processing...';
        
        // Re-enable button after 5 seconds as failsafe
        setTimeout(() => {
            submitBtn.disabled = false;
            submitBtn.classList.remove('loading');
            submitBtn.innerHTML = originalText;
        }, 5000);
    }
});

// Handle responsive navigation
document.addEventListener('click', function(e) {
    if (e.target.matches('.navbar-toggler')) {
        const navbar = document.querySelector('.navbar-collapse');
        if (navbar) {
            navbar.classList.toggle('show');
        }
    }
});

// Auto-close mobile menu when clicking outside
document.addEventListener('click', function(e) {
    const navbar = document.querySelector('.navbar-collapse');
    const toggler = document.querySelector('.navbar-toggler');
    
    if (navbar && navbar.classList.contains('show') && 
        !navbar.contains(e.target) && !toggler.contains(e.target)) {
        navbar.classList.remove('show');
    }
});

// Handle window resize
window.addEventListener('resize', ShrinkURL.debounce(function() {
    // Close mobile menu on resize
    const navbar = document.querySelector('.navbar-collapse');
    if (navbar && window.innerWidth > 992) {
        navbar.classList.remove('show');
    }
}, 250));

// Add to main.js
const AuthManager = {
    logout: function() {
        const token = JWTManager.getToken();
        
        if (token) {
            // Call logout endpoint to invalidate token on server
            fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            })
            .finally(() => {
                // Clear local storage and redirect
                JWTManager.removeTokens();
                window.location.href = '/auth/login';
            });
        } else {
            // Just redirect if no token
            window.location.href = '/auth/login';
        }
    }
};

// Update logout links to use this function
document.addEventListener('DOMContentLoaded', function() {
    const logoutLinks = document.querySelectorAll('a[href*="/auth/logout"], button[data-action="logout"]');
    
    logoutLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            AuthManager.logout();
        });
    });
});