// Dashboard specific JavaScript
let currentPage = 0;
let baseUrl = window.location.protocol + '//' + window.location.host;

// Initialize dashboard functionality
document.addEventListener('DOMContentLoaded', function() {
    console.log('Dashboard JavaScript loaded');
    initializeCopyButtons();
    initializePagination();
    setupTooltips();
    setupLoadMoreButton();
});

// Initialize copy buttons
function initializeCopyButtons() {
    console.log('Initializing copy buttons...');
    const copyButtons = document.querySelectorAll('.copy-btn');
    console.log('Found copy buttons:', copyButtons.length);
    
    // Remove existing listeners and add new ones
    copyButtons.forEach(button => {
        // Clone to remove existing listeners
        // const newButton = button.cloneNode(true);
        // button.parentNode.replaceChild(newButton, button);
        
        // Add click listener
        button.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            const shortUrl = this.getAttribute('data-short-url');
            let fullUrl;
            
            // Check if the short URL already contains the full URL
            if (shortUrl && shortUrl.startsWith('http')) {
                fullUrl = shortUrl;
            } else if (shortUrl) {
                // If it's just a path, add base URL
                fullUrl = baseUrl + shortUrl;
            } else {
                console.error('No short URL found in data attribute');
                return;
            }
            
            console.log('Copy button clicked, URL:', fullUrl);
            copyToClipboard(fullUrl, this);
        });
    });
}

// Initialize pagination
function initializePagination() {
    // Set initial page based on loaded URLs
    const initialUrls = document.querySelectorAll('#urls-tbody tr').length;
    console.log('Initial URLs count:', initialUrls);
    
    // Start from page 0 (first page already loaded)
    currentPage = 0;
    
    // Check if there are more URLs to load
    checkForMoreUrls();
}

// Load more URLs function - simplified
function loadMoreUrls() {
    console.log('loadMoreUrls called');
    const loadingIndicator = document.getElementById('loading-indicator');
    const loadMoreBtn = document.getElementById('load-more-btn');
    
    if (!loadMoreBtn) {
        console.error('Load more button not found');
        return;
    }
    
    // Show loading state
    if (loadingIndicator) {
        loadingIndicator.style.display = 'block';
    }
    loadMoreBtn.disabled = true;
    
    // Calculate next page
    const nextPage = currentPage + 1;
    
    console.log('Loading page:', nextPage);
    
    // Make API call
    fetch(`/api/urls/paginated?page=${nextPage}&size=10`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        console.log('Response data:', data);
        if (data.urls && data.urls.length > 0) {
            appendUrlsToTable(data.urls);
            currentPage = nextPage;
            
            // Hide load more button if no more URLs
            if (!data.hasMore) {
                loadMoreBtn.style.display = 'none';
            }
        } else {
            loadMoreBtn.style.display = 'none';
        }
    })
    .catch(error => {
        console.error('Error loading more URLs:', error);
        showNotification('Error loading more URLs', 'error');
    })
    .finally(() => {
        if (loadingIndicator) {
            loadingIndicator.style.display = 'none';
        }
        loadMoreBtn.disabled = false;
    });
}

// Check if there are more URLs to load
function checkForMoreUrls() {
    const initialUrls = document.querySelectorAll('#urls-tbody tr').length;
    console.log('Checking for more URLs, current count:', initialUrls);
    
    // If we already have 10 or more URLs, check if there are more
    if (initialUrls >= 10) {
        fetch('/api/urls/paginated?page=1&size=10', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Check more URLs response:', data);
            const loadMoreBtn = document.getElementById('load-more-btn');
            if (loadMoreBtn) {
                if (data.hasMore || (data.urls && data.urls.length > 0)) {
                    loadMoreBtn.style.display = 'block';
                } else {
                    loadMoreBtn.style.display = 'none';
                }
            }
        })
        .catch(error => {
            console.error('Error checking for more URLs:', error);
            // Hide button on error
            const loadMoreBtn = document.getElementById('load-more-btn');
            if (loadMoreBtn) {
                loadMoreBtn.style.display = 'none';
            }
        });
    } else {
        // If we have less than 10 URLs, hide the load more button
        const loadMoreBtn = document.getElementById('load-more-btn');
        if (loadMoreBtn) {
            loadMoreBtn.style.display = 'none';
        }
    }
}

// Append URLs to the table
function appendUrlsToTable(urls) {
    const tbody = document.getElementById('urls-tbody');
    
    urls.forEach(url => {
        const row = createUrlRow(url);
        tbody.appendChild(row);
    });
    
    // Reinitialize copy buttons for new rows
    initializeCopyButtons();
}

// Create URL row
function createUrlRow(url) {
    const row = document.createElement('tr');
    row.innerHTML = `
        <td>
            <a href="/${url.shortCode}" target="_blank" class="text-primary fw-bold">
                ${url.shortCode}
            </a>
        </td>
        <td>
            <a href="${url.originalUrl}" target="_blank" class="text-success">
                ${truncateUrl(url.originalUrl, 50)}
            </a>
        </td>
        <td>
            <span class="badge bg-primary">${url.clickCount}</span>
        </td>
        <td>
            <small class="text-muted">${formatDate(url.createdAt)}</small>
        </td>
        <td>
            <button class="btn btn-sm btn-outline-primary copy-btn" 
                    data-short-url="/${url.shortCode}"
                    data-short-code="${url.shortCode}"
                    title="Copy short URL">
                <i class="fas fa-copy"></i>
            </button>
            <a href="/api/urls/${url.id}/analytics-page" 
               class="btn btn-sm btn-outline-info ms-1"
               title="View analytics">
                <i class="fas fa-chart-line"></i>
            </a>
        </td>
    `;
    return row;
}

// Copy to clipboard function
function copyToClipboard(text, button) {
    console.log('Copying to clipboard:', text); // Debug log
    
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(() => {
            showCopySuccess(button);
        }).catch(err => {
            console.error('Failed to copy: ', err);
            fallbackCopy(text, button);
        });
    } else {
        fallbackCopy(text, button);
    }
}

// Fallback copy method
function fallbackCopy(text, button) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
        const successful = document.execCommand('copy');
        if (successful) {
            showCopySuccess(button);
        } else {
            showNotification('Failed to copy URL', 'error');
        }
    } catch (err) {
        console.error('Fallback copy failed:', err);
        showNotification('Failed to copy URL', 'error');
    }
    
    document.body.removeChild(textArea);
}

// Show copy success feedback
function showCopySuccess(button) {
    const originalIcon = button.innerHTML;
    const originalTitle = button.title;
    
    button.innerHTML = '<i class="fas fa-check"></i>';
    button.title = 'Copied!';
    button.classList.remove('btn-outline-primary');
    button.classList.add('btn-success');
    
    setTimeout(() => {
        button.innerHTML = originalIcon;
        button.title = originalTitle;
        button.classList.remove('btn-success');
        button.classList.add('btn-outline-primary');
    }, 2000);
    
    showNotification('URL copied to clipboard!', 'success');
}

// Show notification
function showNotification(message, type = 'info') {
    // Remove existing notifications
    const existingNotifications = document.querySelectorAll('.fixed-notification');
    existingNotifications.forEach(notification => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    });
    
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'info'} alert-dismissible fade show fixed-notification`;
    notification.style.position = 'fixed';
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '1050';
    notification.style.minWidth = '300px';
    notification.style.maxWidth = '400px';
    
    notification.innerHTML = `
        <i class="fas fa-${type === 'error' ? 'exclamation-circle' : type === 'success' ? 'check-circle' : 'info-circle'} me-2"></i>
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    // Auto-remove after 4 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 4000);
}

// Utility functions
function truncateUrl(url, maxLength) {
    if (url.length <= maxLength) {
        return url;
    }
    return url.substring(0, maxLength - 3) + '...';
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Setup tooltips
function setupTooltips() {
    // Initialize Bootstrap tooltips if available
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
}

// Setup load more button - simplified
function setupLoadMoreButton() {
    console.log('Setting up load more button');
    const loadMoreBtn = document.getElementById('load-more-btn');
    
    if (!loadMoreBtn) {
        console.log('Load more button not found');
        return;
    }
    
    // Remove existing listeners by cloning
    const newBtn = loadMoreBtn.cloneNode(true);
    loadMoreBtn.parentNode.replaceChild(newBtn, loadMoreBtn);
    
    // Add click listener
    newBtn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        console.log('Load more button clicked');
        loadMoreUrls();
    });
    
    console.log('Load more button setup complete');
}

// Refresh dashboard data
function refreshDashboard() {
    location.reload();
}

// Make functions globally available
window.loadMoreUrls = loadMoreUrls;
window.refreshDashboard = refreshDashboard;
