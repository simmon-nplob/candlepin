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

import static org.junit.jupiter.api.Assertions.*;

import org.candlepin.test.DatabaseTestFixture;

import org.junit.jupiter.api.Test;

import java.util.Date;

import javax.inject.Inject;

/**
 * ExporterMetadataCuratorTest
 */
public class ExporterMetadataCuratorTest extends DatabaseTestFixture {
    @Inject private ExporterMetadataCurator emc;

    @Test
    public void testCreation() {
        ExporterMetadata em = new ExporterMetadata();
        em.setType(ExporterMetadata.TYPE_SYSTEM);
        em.setExported(new Date());
        assertNull(em.getId());
        ExporterMetadata emdb = emc.create(em);
        assertNotNull(emdb);
        assertNotNull(emdb.getId());
        assertNull(emdb.getOwner());
    }

    @Test
    public void testLookup() {
        ExporterMetadata em = new ExporterMetadata();
        em.setType(ExporterMetadata.TYPE_SYSTEM);
        em.setExported(new Date());
        assertNull(em.getId());
        ExporterMetadata emdb = emc.create(em);
        ExporterMetadata emfound = emc.get(emdb.getId());
        assertNotNull(emfound);
        assertNotNull(emfound.getId());
        assertEquals(emdb.getId(), emfound.getId());
    }

    @Test
    public void getByType() {
        ExporterMetadata em = new ExporterMetadata();
        em.setType(ExporterMetadata.TYPE_SYSTEM);
        em.setExported(new Date());
        assertNull(em.getId());
        ExporterMetadata emdb = emc.create(em);

        assertNull(emc.getByType(ExporterMetadata.TYPE_PER_USER));
        assertEquals(emdb, emc.getByType(ExporterMetadata.TYPE_SYSTEM));
    }

    @Test
    public void setOwner() {
        ExporterMetadata em = new ExporterMetadata();
        em.setType(ExporterMetadata.TYPE_PER_USER);
        em.setExported(new Date());
        em.setOwner(createOwner());
        ExporterMetadata emdb = emc.create(em);
        assertNotNull(emdb);
        assertNotNull(emdb.getOwner());
        assertNotNull(emdb.getOwner().getId());
    }

    @Test
    public void getByTypeAndOwner() {
        ExporterMetadata em = new ExporterMetadata();
        Owner owner = createOwner();
        em.setType(ExporterMetadata.TYPE_PER_USER);
        em.setExported(new Date());
        em.setOwner(owner);
        ExporterMetadata emdb = emc.create(em);

        assertEquals(emdb, emc.getByTypeAndOwner(
            ExporterMetadata.TYPE_PER_USER, owner));
    }
}
