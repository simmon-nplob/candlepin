/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.model;

import static org.junit.Assert.*;

import org.candlepin.test.DatabaseTestFixture;

import org.junit.Test;

import javax.inject.Inject;


/**
 * ProductTest
 */
public class ProductTest extends DatabaseTestFixture {
    @Inject private OwnerCurator ownerCurator;

    @Test
    public void testLockStateAffectsEquality() {
        Owner owner = new Owner("Example-Corporation");
        Product p1 = new Product("test-prod", "test-prod-name", "variant", "1.0.0", "x86", "type");
        Product p2 = new Product("test-prod", "test-prod-name", "variant", "1.0.0", "x86", "type");

        assertEquals(p1, p2);

        p2.setLocked(true);
        assertNotEquals(p1, p2);

        p1.setLocked(true);
        assertEquals(p1, p2);
    }

    @Test
    public void testLockStateAffectsHashCode() {
        Owner owner = new Owner("Example-Corporation");
        Product p1 = new Product("test-prod", "test-prod-name", "variant", "1.0.0", "x86", "type");
        Product p2 = new Product("test-prod", "test-prod-name", "variant", "1.0.0", "x86", "type");

        assertEquals(p1.hashCode(), p2.hashCode());

        p2.setLocked(true);
        assertNotEquals(p1.hashCode(), p2.hashCode());

        p1.setLocked(true);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
