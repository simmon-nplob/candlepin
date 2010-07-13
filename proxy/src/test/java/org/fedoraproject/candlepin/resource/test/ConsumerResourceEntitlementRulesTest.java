/**
 * Copyright (c) 2009 Red Hat, Inc.
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
package org.fedoraproject.candlepin.resource.test;

import org.fedoraproject.candlepin.auth.Principal;
import org.fedoraproject.candlepin.exceptions.ForbiddenException;
import org.fedoraproject.candlepin.model.Consumer;
import org.fedoraproject.candlepin.model.ConsumerType;
import org.fedoraproject.candlepin.model.Owner;
import org.fedoraproject.candlepin.model.Pool;
import org.fedoraproject.candlepin.model.Product;
import org.fedoraproject.candlepin.policy.Enforcer;
import org.fedoraproject.candlepin.policy.js.entitlement.JavascriptEnforcer;
import org.fedoraproject.candlepin.resource.ConsumerResource;
import org.fedoraproject.candlepin.test.DatabaseTestFixture;
import org.fedoraproject.candlepin.test.TestDateUtil;
import org.fedoraproject.candlepin.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

/**
 * ConsumerResourceEntitlementRulesTest
 */
public class ConsumerResourceEntitlementRulesTest extends DatabaseTestFixture {
    private ConsumerType standardSystemType;
    private Consumer consumer;
    private Product product;
    private Pool pool;
    
    private ConsumerResource consumerResource;
    private Principal principal;
    private Owner owner;

    @Before
    public void setUp() {
        principal = injector.getInstance(Principal.class);
        consumerResource = injector.getInstance(ConsumerResource.class);
        
        standardSystemType = consumerTypeCurator.create(
                new ConsumerType("standard-system"));
        owner = ownerCurator.create(new Owner("test-owner"));
        ownerCurator.create(owner);
        
        consumer = TestUtil.createConsumer(standardSystemType, owner);
        consumerCurator.create(consumer);
        
        product = TestUtil.createProduct();
        productCurator.create(product);
        
        pool = createPoolAndSub(owner, product, new Long(10),
            TestDateUtil.date(2010, 1, 1), TestDateUtil.date(2020, 12, 31));
        poolCurator.create(pool);
    }
    
    @Test(expected = ForbiddenException.class)
    public void testMaxMembership() {
        // 10 entitlements available, lets try to entitle 11 consumers.
        for (int i = 0; i < pool.getQuantity(); i++) {
            Consumer c = TestUtil.createConsumer(consumer.getType(), owner);
            consumerCurator.create(c);
            consumerResource.bind(
                c.getUuid(), null, null, product.getId(), new Integer(1), null, null);
        }
        
        // Now for the 11th:
        Consumer c = TestUtil.createConsumer(consumer.getType(), owner);
        consumerCurator.create(c);
        consumerResource.bind(
            c.getUuid(), null, null, product.getId(), new Integer(1), null, null);
    }
    
    @Test(expected = RuntimeException.class)
    public void testEntitlementsHaveExpired() {
        dateSource.currentDate(TestDateUtil.date(2030, 1, 13));
        consumerResource.bind(consumer.getUuid(), null, null,
            product.getId(), null, null, null);
    }
    
    @Override
    protected Module getGuiceOverrideModule() {
        return new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Enforcer.class).to(JavascriptEnforcer.class);
            }
        };
    }
}
