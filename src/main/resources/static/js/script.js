document.addEventListener('DOMContentLoaded', function() {
    console.log('ShrinkURL application loaded!');

    // Example of a simple JavaScript interaction
    // You might add logic for form submissions, animations, etc. here.

    // Get the current year and update the footer dynamically (though Thymeleaf does this too)
    const currentYearSpan = document.querySelector('footer p span');
    if (currentYearSpan) {
        currentYearSpan.textContent = new Date().getFullYear();
    }
});