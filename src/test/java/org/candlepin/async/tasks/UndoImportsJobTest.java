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
package org.candlepin.async.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.async.JobConfig;
import org.candlepin.async.JobExecutionContext;
import org.candlepin.async.JobExecutionException;
import org.candlepin.controller.PoolService;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.Entitlement;
import org.candlepin.model.ExporterMetadata;
import org.candlepin.model.ExporterMetadataCurator;
import org.candlepin.model.ImportRecord;
import org.candlepin.model.ImportRecordCurator;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.Pool;
import org.candlepin.model.Pool.PoolType;
import org.candlepin.model.Product;
import org.candlepin.model.UeberCertificate;
import org.candlepin.model.UeberCertificateGenerator;
import org.candlepin.model.UpstreamConsumer;
import org.candlepin.test.DatabaseTestFixture;
import org.candlepin.test.TestUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;



public class UndoImportsJobTest extends DatabaseTestFixture {

    protected I18n i18n;
    protected ImportRecordCurator importRecordCurator;
    protected UeberCertificateGenerator ueberCertGenerator;
    protected OwnerCurator ownerCurator;
    protected ExporterMetadataCurator exportCurator;
    protected PoolService poolService;

    private UndoImportsJob undoImportsJob;

    @BeforeEach
    public void setUp() {
        this.i18n = I18nFactory.getI18n(this.getClass(), Locale.US, I18nFactory.FALLBACK);
        poolService = this.injector.getInstance(PoolService.class);
        ueberCertGenerator = this.injector.getInstance(UeberCertificateGenerator.class);
        ownerCurator = this.injector.getInstance(OwnerCurator.class);
        exportCurator = this.injector.getInstance(ExporterMetadataCurator.class);
        importRecordCurator = this.injector.getInstance(ImportRecordCurator.class);
        this.undoImportsJob = new UndoImportsJob(
            this.i18n, this.ownerCurator, this.poolService,
            this.exportCurator, this.importRecordCurator, this.config);
    }

    @Test
    public void testUndoImport() throws JobExecutionException {
        this.undoImportsJob = new UndoImportsJob(
            this.i18n, this.ownerCurator, this.poolService, this.exportCurator,
            this.importRecordCurator, this.config);

        // Create owner w/upstream consumer
        Owner owner1 = TestUtil.createOwner();
        Owner owner2 = TestUtil.createOwner();
        owner1.setId(null);
        owner2.setId(null);

        ConsumerType type = this.createConsumerType();
        UpstreamConsumer uc1 = new UpstreamConsumer("uc1", null, type, "uc1");
        UpstreamConsumer uc2 = new UpstreamConsumer("uc2", null, type, "uc2");
        this.ownerCurator.create(owner1);
        this.ownerCurator.create(owner2);
        owner1.setUpstreamConsumer(uc1);
        owner1.setUpstreamConsumer(uc2);
        this.ownerCurator.merge(owner1);
        this.ownerCurator.merge(owner2);

        // Create metadata
        ExporterMetadata metadata1 = new ExporterMetadata(ExporterMetadata.TYPE_PER_USER, new Date(), owner1);
        ExporterMetadata metadata2 = new ExporterMetadata(ExporterMetadata.TYPE_PER_USER, new Date(), owner2);
        this.exporterMetadataCurator.create(metadata1);
        this.exporterMetadataCurator.create(metadata2);

        // Create pools w/upstream pool IDs
        Pool pool1 = this.createPool("pool1", owner1, true, PoolType.NORMAL);
        Pool pool2 = this.createPool("pool2", owner1, true, PoolType.BONUS);
        Pool pool3 = this.createPool("pool3", owner1, false, PoolType.NORMAL);
        Pool pool4 = this.createPool("pool4", owner1, false, PoolType.BONUS);
        Pool pool5 = this.createPool("pool5", owner1, true, PoolType.ENTITLEMENT_DERIVED);
        Pool pool6 = this.createPool("pool6", owner1, false, PoolType.ENTITLEMENT_DERIVED);
        Pool pool7 = this.createPool("pool7", owner2, true, PoolType.NORMAL);
        Pool pool8 = this.createPool("pool8", owner2, true, PoolType.BONUS);
        Pool pool9 = this.createPool("pool9", owner2, true, PoolType.ENTITLEMENT_DERIVED);

        // Create an ueber certificate for the owner.
        UeberCertificate uebercert = ueberCertGenerator.generate(owner1.getKey(),
            this.setupAdminPrincipal("test_admin"));
        assertNotNull(uebercert);

        // Verify initial state
        assertEquals(
            Arrays.asList(pool1, pool2, pool3, pool4, pool5, pool6),
            this.poolService.listPoolsByOwner(owner1).list());
        assertEquals(Arrays.asList(pool7, pool8, pool9), this.poolService.listPoolsByOwner(owner2).list());
        assertEquals(metadata1, exportCurator.getByTypeAndOwner(ExporterMetadata.TYPE_PER_USER, owner1));
        assertEquals(metadata2, exportCurator.getByTypeAndOwner(ExporterMetadata.TYPE_PER_USER, owner2));
        assertEquals(0, this.importRecordCurator.findRecords(owner1).list().size());
        assertEquals(0, this.importRecordCurator.findRecords(owner2).list().size());

        JobConfig config = UndoImportsJob.createJobConfig()
            .setOwner(owner1);

        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobArguments()).thenReturn(config.getJobArguments());
        when(context.getPrincipalName()).thenReturn("JarJarBinks");

        // Execute job
        beginTransaction(); // since we locking owner we need start transaction
        this.undoImportsJob.execute(context);
        commitTransaction();

        // Verify deletions -- Ueber pools should not get deleted.
        assertEquals(List.of(pool3, pool4, pool5, pool6),
            this.poolService.listPoolsByOwner(owner1).list());

        assertEquals(Arrays.asList(pool7, pool8, pool9), this.poolService.listPoolsByOwner(owner2).list());
        assertNull(exportCurator.getByTypeAndOwner(ExporterMetadata.TYPE_PER_USER, owner1));
        assertEquals(metadata2, exportCurator.getByTypeAndOwner(ExporterMetadata.TYPE_PER_USER, owner2));
        assertNull(owner1.getUpstreamConsumer());

        List<ImportRecord> records = this.importRecordCurator.findRecords(owner1).list();
        assertEquals(1, records.size());
        assertEquals(ImportRecord.Status.DELETE, records.get(0).getStatus());

        assertEquals(0, this.importRecordCurator.findRecords(owner2).list().size());
    }

    protected Pool createPool(String name, Owner owner, boolean keepSourceSub, PoolType type) {
        Product product = this.createProduct(name, name);

        Pool pool = TestUtil.createPool(owner, product);

        if (!keepSourceSub) {
            pool.setSourceSubscription(null);
        }
        else {
            pool.setUpstreamPoolId("upstream_pool_id")
                .setManaged(true);
        }

        this.poolCurator.create(pool);

        Consumer consumer;
        Entitlement entitlement;

        switch (type) {
            // TODO: Others as necessary

            case ENTITLEMENT_DERIVED:
                pool.setAttribute(Pool.Attributes.DERIVED_POOL, "true");
                consumer = this.createConsumer(owner);
                entitlement = this.createEntitlement(owner, consumer, pool, null);

                pool.setSourceEntitlement(entitlement);
                break;

            case BONUS:
                pool.setAttribute(Pool.Attributes.DERIVED_POOL, "true");
                break;

            default:
                // Required by checkstyle.
        }

        this.poolCurator.merge(pool);

        return pool;
    }
}
