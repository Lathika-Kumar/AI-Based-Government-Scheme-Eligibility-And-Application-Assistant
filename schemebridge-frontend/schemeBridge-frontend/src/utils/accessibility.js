/**
 * Accessibility utilities for ARIA labels, keyboard navigation, and screen reader support
 */

/**
 * Generate unique ID for ARIA relationships
 * @returns {string} Unique ID
 */
export function generateAriaId(prefix = "aria") {
  return `${prefix}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Get ARIA attributes for form fields with errors
 * @param {boolean} hasError - Whether the field has an error
 * @param {string} errorId - ID of the error message element
 * @returns {Object} ARIA attributes
 */
export function getFieldErrorAria(hasError, errorId) {
  return {
    "aria-invalid": hasError,
    "aria-describedby": hasError ? errorId : undefined,
  };
}

/**
 * Get ARIA attributes for live regions
 * @param {string} politeness - "polite", "assertive", or "off"
 * @returns {Object} ARIA live region attributes
 */
export function getLiveRegionAria(politeness = "polite") {
  return {
    "aria-live": politeness,
    "aria-atomic": "true",
  };
}

/**
 * Get ARIA attributes for modal dialogs
 * @param {boolean} isOpen - Whether the modal is open
 * @param {string} label - Accessible label for the modal
 * @returns {Object} ARIA modal attributes
 */
export function getModalAria(isOpen, label) {
  return {
    role: "dialog",
    "aria-modal": isOpen,
    "aria-labelledby": label,
  };
}

/**
 * Get ARIA attributes for expandable/collapsible content
 * @param {boolean} isExpanded - Whether content is expanded
 * @param {string} controlsId - ID of the controlled element
 * @returns {Object} ARIA expandable attributes
 */
export function getExpandableAria(isExpanded, controlsId) {
  return {
    "aria-expanded": isExpanded,
    "aria-controls": controlsId,
  };
}

/**
 * Get ARIA attributes for tabs
 * @param {boolean} isSelected - Whether tab is selected
 * @param {string} panelId - ID of the associated panel
 * @returns {Object} ARIA tab attributes
 */
export function getTabAria(isSelected, panelId) {
  return {
    role: "tab",
    "aria-selected": isSelected,
    "aria-controls": panelId,
    tabIndex: isSelected ? 0 : -1,
  };
}

/**
 * Get ARIA attributes for tab panels
 * @param {string} tabId - ID of the associated tab
 * @param {boolean} isSelected - Whether panel is selected
 * @returns {Object} ARIA tab panel attributes
 */
export function getTabPanelAria(tabId, isSelected) {
  return {
    role: "tabpanel",
    "aria-labelledby": tabId,
    hidden: !isSelected,
  };
}

/**
 * Get ARIA attributes for progress indicators
 * @param {number} value - Current value
 * @param {number} min - Minimum value
 * @param {number} max - Maximum value
 * @param {string} label - Accessible label
 * @returns {Object} ARIA progress attributes
 */
export function getProgressAria(value, min = 0, max = 100, label) {
  const percentage = Math.round(((value - min) / (max - min)) * 100);
  return {
    role: "progressbar",
    "aria-valuenow": value,
    "aria-valuemin": min,
    "aria-valuemax": max,
    "aria-valuetext": `${percentage}% complete`,
    "aria-label": label,
  };
}

/**
 * Get ARIA attributes for loading states
 * @param {string} label - Accessible label
 * @returns {Object} ARIA loading attributes
 */
export function getLoadingAria(label = "Loading") {
  return {
    role: "status",
    "aria-live": "polite",
    "aria-busy": "true",
    "aria-label": label,
  };
}

/**
 * Get ARIA attributes for tooltips
 * @param {string} content - Tooltip content
 * @param {string} id - ID of the tooltip element
 * @returns {Object} ARIA tooltip attributes
 */
export function getTooltipAria(content, id) {
  return {
    "aria-describedby": id,
    "aria-label": content,
  };
}

/**
 * Get ARIA attributes for navigation menus
 * @param {boolean} isExpanded - Whether menu is expanded
 * @returns {Object} ARIA navigation attributes
 */
export function getNavigationAria(isExpanded) {
  return {
    role: "navigation",
    "aria-expanded": isExpanded,
  };
}

/**
 * Get ARIA attributes for buttons with icons
 * @param {string} label - Accessible label for the button
 * @param {boolean} isPressed - Whether button is pressed (toggle buttons)
 * @returns {Object} ARIA button attributes
 */
export function getIconButtonAria(label, isPressed = undefined) {
  const attributes = {
    "aria-label": label,
  };

  if (isPressed !== undefined) {
    attributes["aria-pressed"] = isPressed;
  }

  return attributes;
}

/**
 * Get ARIA attributes for checkboxes
 * @param {boolean} isChecked - Whether checkbox is checked
 * @param {string} label - Accessible label
 * @returns {Object} ARIA checkbox attributes
 */
export function getCheckboxAria(isChecked, label) {
  return {
    role: "checkbox",
    "aria-checked": isChecked,
    "aria-label": label,
  };
}

/**
 * Get ARIA attributes for radio buttons
 * @param {boolean} isSelected - Whether radio is selected
 * @param {string} label - Accessible label
 * @param {string} name - Radio group name
 * @returns {Object} ARIA radio attributes
 */
export function getRadioAria(isSelected, label, name) {
  return {
    role: "radio",
    "aria-checked": isSelected,
    "aria-label": label,
    name,
  };
}

/**
 * Get ARIA attributes for alerts
 * @param {string} type - "info", "success", "warning", "error"
 * @returns {Object} ARIA alert attributes
 */
export function getAlertAria(type = "info") {
  const roles = {
    info: "status",
    success: "status",
    warning: "alert",
    error: "alert",
  };

  return {
    role: roles[type] || "status",
    "aria-live": type === "error" || type === "warning" ? "assertive" : "polite",
  };
}

/**
 * Handle keyboard navigation for custom components
 * @param {KeyboardEvent} event - Keyboard event
 * @param {Object} handlers - Handler functions for different keys
 */
export function handleKeyboardNavigation(event, handlers) {
  const {
    onEnter,
    onSpace,
    onEscape,
    onArrowUp,
    onArrowDown,
    onArrowLeft,
    onArrowRight,
    onHome,
    onEnd,
    onTab,
  } = handlers;

  switch (event.key) {
    case "Enter":
      if (onEnter) {
        event.preventDefault();
        onEnter(event);
      }
      break;
    case " ":
      if (onSpace) {
        event.preventDefault();
        onSpace(event);
      }
      break;
    case "Escape":
      if (onEscape) {
        event.preventDefault();
        onEscape(event);
      }
      break;
    case "ArrowUp":
      if (onArrowUp) {
        event.preventDefault();
        onArrowUp(event);
      }
      break;
    case "ArrowDown":
      if (onArrowDown) {
        event.preventDefault();
        onArrowDown(event);
      }
      break;
    case "ArrowLeft":
      if (onArrowLeft) {
        event.preventDefault();
        onArrowLeft(event);
      }
      break;
    case "ArrowRight":
      if (onArrowRight) {
        event.preventDefault();
        onArrowRight(event);
      }
      break;
    case "Home":
      if (onHome) {
        event.preventDefault();
        onHome(event);
      }
      break;
    case "End":
      if (onEnd) {
        event.preventDefault();
        onEnd(event);
      }
      break;
    case "Tab":
      if (onTab) {
        onTab(event);
      }
      break;
  }
}

/**
 * Focus trap for modals and dialogs
 * @param {HTMLElement} container - Container element to trap focus within
 * @returns {Function} Cleanup function to remove focus trap
 */
export function createFocusTrap(container) {
  const focusableElements = container.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );
  const firstElement = focusableElements[0];
  const lastElement = focusableElements[focusableElements.length - 1];

  const handleTab = (e) => {
    if (e.key !== "Tab") {
return;
}

    if (e.shiftKey) {
      if (document.activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      }
    } else {
      if (document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    }
  };

  container.addEventListener("keydown", handleTab);
  firstElement?.focus();

  return () => {
    container.removeEventListener("keydown", handleTab);
  };
}

/**
 * Announce message to screen readers
 * @param {string} message - Message to announce
 * @param {string} politeness - "polite" or "assertive"
 */
export function announceToScreenReader(message, politeness = "polite") {
  const announcement = document.createElement("div");
  announcement.setAttribute("role", "status");
  announcement.setAttribute("aria-live", politeness);
  announcement.setAttribute("aria-atomic", "true");
  announcement.className = "sr-only";
  announcement.style.cssText = `
    position: absolute;
    left: -10000px;
    width: 1px;
    height: 1px;
    overflow: hidden;
  `;
  announcement.textContent = message;

  document.body.appendChild(announcement);

  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 1000);
}

/**
 * Skip to main content link
 * @returns {JSX.Element} Skip link component
 */
export function SkipLink() {
  return (
    <a
      href="#main-content"
      className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:bg-indigo-600 focus:text-white focus:px-4 focus:py-2 focus:rounded-lg"
      style={{
        position: "absolute",
        left: "-10000px",
        width: "1px",
        height: "1px",
        overflow: "hidden",
      }}
    >
      Skip to main content
    </a>
  );
}
