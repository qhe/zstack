package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageEO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l3.IpRangeEO;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 *
 * delete backup storage
 */
public class TestCascadeDeletion6 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        BackupStorageInventory bsinv = deployer.backupStorages.get("TestBackupStorage");
        DiskOfferingInventory do1 = deployer.diskOfferings.get("TestRootDiskOffering");
        DiskOfferingInventory do2 = deployer.diskOfferings.get("TestDataDiskOffering");
        InstanceOfferingInventory io = deployer.instanceOfferings.get("TestInstanceOffering");
        BackupStorageInventory bs = deployer.backupStorages.get("TestBackupStorage");

        api.deleteBackupStorage(bsinv.getUuid());
        long count = dbf.count(ZoneVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(BackupStorageZoneRefVO.class);
        Assert.assertTrue(0 == count);
        count = dbf.count(ClusterVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(HostVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(VmInstanceVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(PrimaryStorageVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(L2NetworkVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(L3NetworkVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(IpRangeVO.class);
        Assert.assertTrue(0 != count);
        count = dbf.count(ImageVO.class);
        Assert.assertTrue(0 == count);
        count = dbf.count(ImageBackupStorageRefVO.class);
        Assert.assertTrue(0 == count);
        DiskOfferingVO dvo = dbf.findByUuid(do1.getUuid(), DiskOfferingVO.class);
        Assert.assertNotNull(dvo);
        dvo = dbf.findByUuid(do2.getUuid(), DiskOfferingVO.class);
        Assert.assertNotNull(dvo);
        InstanceOfferingVO ivo = dbf.findByUuid(io.getUuid(), InstanceOfferingVO.class);
        Assert.assertNotNull(ivo);
        BackupStorageVO bvo = dbf.findByUuid(bs.getUuid(), BackupStorageVO.class);
        Assert.assertNull(bvo);

        CascadeTestHelper helper = new CascadeTestHelper();
        helper.zeroInDatabase(
                BackupStorageEO.class
        );
    }
}
