/*
 * Copyright (c) 2009 - 2023 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.policy;

import org.xnap.commons.i18n.I18n;

/**
 * Represents a validation error generated by rules processing.
 */
public interface RulesValidationError {

    /**
     * Builds a translated error message for the validation error that this object represents.
     * @param i18n object that provides internationalization
     * @param args positional object arguments for the error message
     * @return the final translated error message in text
     */
    String buildErrorMessage(I18n i18n, Object... args);
}
