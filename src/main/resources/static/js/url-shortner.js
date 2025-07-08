// URL Shortener specific functionality
const URLShortener = {
    form: null,
    resultSection: null,
    originalUrlInput: null,
    shortenedUrlInput: null,
    shortenBtn: null,
    copyBtn: null,

    init: function() {
        this.bindElements();
        this.bindEvents();
        this.loadRecentUrls();
    },

    bindElements: function() {
        this.form = document.getElementById('urlShortenerForm');
        this.resultSection = document.getElementById('resultSection');
        this.originalUrlInput = document.getElementById('originalUrl');
        this.shortenedUrlInput = document.getElementById('shortenedUrl');
        this.shortenBtn = document.getElementById('shortenBtn');
        this.copyBtn = document.getElementById('copyBtn');
    },

    bindEvents: function() {
        if (this.form) {
            this.form.addEventListener('submit', this.handleSubmit.bind(this));
        }

        if (this.originalUrlInput) {
            this.originalUrlInput.addEventListener('input', this.validateUrl.bind(this));
            this.originalUrlInput.addEventListener('paste', this.handlePaste.bind(this));
        }

        if (this.copyBtn) {
            this.copyBtn.addEventListener('click', this.copyUrl.bind(this));
        }

        // Real-time validation
        if (this.originalUrlInput) {
            this.originalUrlInput.addEventListener('input', 
                ShrinkURL.debounce(this.validateUrl.bind(this), 300)
            );
        }
    },

    handleSubmit: function(e) {
        e.preventDefault();
        
        const url = this.originalUrlInput.value.trim();
        
        if (!this.isValidUrl(url)) {
            this.showError('Please enter a valid URL');
            return;
        }

        this.shortenUrl(url);
    },

    handlePaste: function(e) {
        // Handle paste event and auto-validate
        setTimeout(() => {
            this.validateUrl();
        }, 10);
    },

    shortenUrl: function(url) {
        // Show loading state
        this.setLoading(true);
        this.hideResult();

        const requestData = {
            originalUrl: url,
            customAlias: this.getCustomAlias(),
            expirationDate: this.getExpirationDate()
        };

        ShrinkURL.makeRequest('/api/shorten', {
            method: 'POST',
            body: JSON.stringify(requestData)
        })
        .then(response => {
            this.handleSuccess(response);
        })
        .catch(error => {
            this.handleError(error);
        })
        .finally(() => {
            this.setLoading(false);
        });
    },

    handleSuccess: function(response) {
        if (response.success) {
            this.shortenedUrlInput.value = response.shortenedUrl;
            this.showResult();
            this.addToRecentUrls(response);
            this.updateStats();
            ShrinkURL.showToast('URL shortened successfully!', 'success');
        } else {
            this.showError(response.message || 'Failed to shorten URL');
        }
    },

    handleError: function(error) {
        console.error('Error shortening URL:', error);
        
        let errorMessage = 'An error occurred while shortening the URL';
        
        if (error.message) {
            if (error.message.includes('400')) {
                errorMessage = 'Invalid URL provided';
            } else if (error.message.includes('429')) {
                errorMessage = 'Rate limit exceeded. Please try again later';
            } else if (error.message.includes('500')) {
                errorMessage = 'Server error. Please try again';
            }
        }
        
        this.showError(errorMessage);
    },

    validateUrl: function() {
        const url = this.originalUrlInput.value.trim();
        
        if (!url) {
            this.clearValidation();
            return;
        }

        if (this.isValidUrl(url)) {
            this.showValidation(true);
            this.shortenBtn.disabled = false;
        } else {
            this.showValidation(false);
            this.shortenBtn.disabled = true;
        }
    },

    isValidUrl: function(string) {
        try {
            const url = new URL(string);
            return url.protocol === 'http:' || url.protocol === 'https:';
        } catch (_) {
            return false;
        }
    },

    showValidation: function(isValid) {
        if (isValid) {
            this.originalUrlInput.classList.remove('is-invalid');
            this.originalUrlInput.classList.add('is-valid');
        } else {
            this.originalUrlInput.classList.remove('is-valid');
            this.originalUrlInput.classList.add('is-invalid');
        }
    },

    clearValidation: function() {
        this.originalUrlInput.classList.remove('is-valid', 'is-invalid');
        this.shortenBtn.disabled = false;
    },

    showResult: function() {
        this.resultSection.style.display = 'block';
        this.resultSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    },

    hideResult: function() {
        this.resultSection.style.display = 'none';
    },

    showError: function(message) {
        ShrinkURL.showToast(message, 'error');
        this.hideResult();
    },

    setLoading: function(isLoading) {
        if (isLoading) {
            this.shortenBtn.disabled = true;
            this.shortenBtn.innerHTML = '<span class="spinner"></span> Shortening...';
            this.originalUrlInput.disabled = true;
        } else {
            this.shortenBtn.disabled = false;
            this.shortenBtn.innerHTML = '<i class="fas fa-magic"></i> Shorten URL';
            this.originalUrlInput.disabled = false;
        }
    },

    copyUrl: function() {
        if (this.shortenedUrlInput) {
            this.shortenedUrlInput.select();
            this.shortenedUrlInput.setSelectionRange(0, 99999);
            
            try {
                document.execCommand('copy');
                this.showCopySuccess();
                
                // Track copy event
                this.trackEvent('copy', {
                    url: this.shortenedUrlInput.value
                });
            } catch (err) {
                ShrinkURL.showToast('Failed to copy URL', 'error');
            }
        }
    },

    showCopySuccess: function() {
        const originalText = this.copyBtn.innerHTML;
        this.copyBtn.innerHTML = '<i class="fas fa-check"></i> Copied!';
        this.copyBtn.classList.add('btn-success');
        this.copyBtn.classList.remove('btn-outline-primary');
        
        setTimeout(() => {
            this.copyBtn.innerHTML = originalText;
            this.copyBtn.classList.remove('btn-success');
            this.copyBtn.classList.add('btn-outline-primary');
        }, 2000);
    },

    getCustomAlias: function() {
        const customAliasInput = document.getElementById('customAlias');
        return customAliasInput ? customAliasInput.value.trim() : '';
    },

    getExpirationDate: function() {
        const expirationInput = document.getElementById('expirationDate');
        return expirationInput ? expirationInput.value : null;
    },

    addToRecentUrls: function(urlData) {
        // Add to recent URLs list (if user is authenticated)
        const recentUrlsList = document.getElementById('recentUrlsList');
        if (recentUrlsList) {
            const urlItem = this.createUrlItem(urlData);
            recentUrlsList.insertBefore(urlItem, recentUrlsList.firstChild);
            
            // Keep only the last 5 items
            const items = recentUrlsList.children;
            if (items.length > 5) {
                recentUrlsList.removeChild(items[items.length - 1]);
            }
        }
    },

    createUrlItem: function(urlData) {
        const div = document.createElement('div');
        div.className = 'url-item p-3 mb-2 bg-light rounded';
        div.innerHTML = `
            <div class="d-flex justify-content-between align-items-center">
                <div class="url-info">
                    <h6 class="mb-1">${this.truncateUrl(urlData.originalUrl)}</h6>
                    <small class="text-muted">${urlData.shortenedUrl}</small>
                </div>
                <div class="url-actions">
                    <button class="btn btn-sm btn-outline-primary" onclick="URLShortener.copyToClipboard('${urlData.shortenedUrl}')">
                        <i class="fas fa-copy"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-info" onclick="URLShortener.showStats('${urlData.id}')">
                        <i class="fas fa-chart-bar"></i>
                    </button>
                </div>
            </div>
        `;
        return div;
    },

    truncateUrl: function(url, maxLength = 50) {
        return url.length > maxLength ? url.substring(0, maxLength) + '...' : url;
    },

    loadRecentUrls: function() {
        // Load recent URLs for authenticated users
        const recentUrlsList = document.getElementById('recentUrlsList');
        if (recentUrlsList) {
            ShrinkURL.makeRequest('/api/recent-urls')
                .then(response => {
                    if (response.success) {
                        this.renderRecentUrls(response.data);
                    }
                })
                .catch(error => {
                    console.error('Failed to load recent URLs:', error);
                });
        }
    },

    renderRecentUrls: function(urls) {
        const recentUrlsList = document.getElementById('recentUrlsList');
        if (recentUrlsList) {
            recentUrlsList.innerHTML = '';
            urls.forEach(url => {
                const urlItem = this.createUrlItem(url);
                recentUrlsList.appendChild(urlItem);
            });
        }
    },

    updateStats: function() {
        // Update user statistics
        const statElements = document.querySelectorAll('[data-stat]');
        if (statElements.length > 0) {
            ShrinkURL.makeRequest('/api/user-stats')
                .then(response => {
                    if (response.success) {
                        statElements.forEach(el => {
                            const statType = el.getAttribute('data-stat');
                            if (response.data[statType] !== undefined) {
                                el.textContent = ShrinkURL.formatNumber(response.data[statType]);
                            }
                        });
                    }
                })
                .catch(error => {
                    console.error('Failed to update stats:', error);
                });
        }
    },

    copyToClipboard: function(url) {
        navigator.clipboard.writeText(url).then(() => {
            ShrinkURL.showToast('URL copied to clipboard!', 'success');
        }).catch(err => {
            ShrinkURL.showToast('Failed to copy URL', 'error');
        });
    },

    showStats: function(urlId) {
        // Show URL statistics in a modal
        ShrinkURL.makeRequest(`/api/url-stats/${urlId}`)
            .then(response => {
                if (response.success) {
                    this.displayStatsModal(response.data);
                }
            })
            .catch(error => {
                ShrinkURL.showToast('Failed to load statistics', 'error');
            });
    },

    displayStatsModal: function(stats) {
        // Create and show statistics modal
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.innerHTML = `
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">URL Statistics</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="stat-item text-center">
                                    <h3 class="text-primary">${stats.totalClicks}</h3>
                                    <small class="text-muted">Total Clicks</small>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="stat-item text-center">
                                    <h3 class="text-success">${stats.todayClicks}</h3>
                                    <small class="text-muted">Today's Clicks</small>
                                </div>
                            </div>
                        </div>
                        <hr>
                        <h6>Recent Activity</h6>
                        <div class="activity-list">
                            ${stats.recentClicks.map(click => `
                                <div class="activity-item d-flex justify-content-between">
                                    <span>${click.country || 'Unknown'}</span>
                                    <small class="text-muted">${new Date(click.timestamp).toLocaleString()}</small>
                                </div>
                            `).join('')}
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
        
        // Remove modal from DOM after hiding
        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
        });
    },

    trackEvent: function(eventType, data) {
        // Track user events for analytics
        ShrinkURL.makeRequest('/api/track-event', {
            method: 'POST',
            body: JSON.stringify({
                event: eventType,
                data: data,
                timestamp: new Date().toISOString()
            })
        }).catch(error => {
            console.error('Failed to track event:', error);
        });
    }
};

// Initialize URL Shortener when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    URLShortener.init();
});

// Export for global access
window.URLShortener = URLShortener;