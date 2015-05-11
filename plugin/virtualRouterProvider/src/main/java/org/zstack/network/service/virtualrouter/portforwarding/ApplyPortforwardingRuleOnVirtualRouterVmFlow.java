package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.Flow;
import org.zstack.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.CreatePortForwardingRuleRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RevokePortForwardingRuleRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplyPortforwardingRuleOnVirtualRouterVmFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(ApplyPortforwardingRuleOnVirtualRouterVmFlow.class);

    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    protected VirtualRouterPortForwardingBackend backend;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private final static String VR_APPLY_PORT_FORWARDING_RULE_SUCCESS = "ApplyPortForwardingRuleSuccess";

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final PortForwardingRuleTO to = (PortForwardingRuleTO) data.get(VirtualRouterConstant.VR_PORT_FORWARDING_RULE);
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.VR_RESULT_VM);

        VirtualRouterCommands.CreatePortForwardingRuleCmd cmd = new VirtualRouterCommands.CreatePortForwardingRuleCmd();
        cmd.setRules(Arrays.asList(to));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                CreatePortForwardingRuleRsp ret = re.toResponse(CreatePortForwardingRuleRsp.class);
                if (ret.isSuccess()) {
                    String info = String
                            .format("successfully create port forwarding rule[vip ip: %s, private ip: %s, vip start port: %s, vip end port: %s, private start port: %s, private end port: %s]",
                                    to.getVipIp(), to.getPrivateIp(), to.getVipPortStart(), to.getVipPortEnd(),
                                    to.getPrivatePortStart(), to.getPrivatePortEnd());
                    logger.debug(info);
                    data.put(VR_APPLY_PORT_FORWARDING_RULE_SUCCESS, Boolean.TRUE);
                    chain.next();
                } else {
                    String err = String
                            .format("failed to create port forwarding rule[vip ip: %s, private ip: %s, vip start port: %s, vip end port: %s, private start port: %s, private end port: %s], because %s",
                                    to.getVipIp(), to.getPrivateIp(), to.getVipPortStart(), to.getVipPortEnd(),
                                    to.getPrivatePortStart(), to.getPrivatePortEnd(), ret.getError());
                    logger.warn(err);
                    chain.fail(errf.stringToOperationError(err));
                }
            }
        });
    }

    @Override
    public void rollback(final FlowTrigger chain, Map data) {
        if (data.get(VR_APPLY_PORT_FORWARDING_RULE_SUCCESS) != null) {
            final PortForwardingRuleTO to = (PortForwardingRuleTO) data.get(VirtualRouterConstant.VR_PORT_FORWARDING_RULE);
            final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.VR_RESULT_VM);

            VirtualRouterCommands.RevokePortForwardingRuleCmd cmd = new VirtualRouterCommands.RevokePortForwardingRuleCmd();
            cmd.setRules(Arrays.asList(to));

            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setCheckStatus(true);
            msg.setPath(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING);
            msg.setCommand(cmd);
            msg.setVmInstanceUuid(vr.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(msg, new CloudBusCallBack(chain) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        String err = String.format("failed to revoke port forwarding rules %, because %s", JSONObjectUtil.toJsonString(to), reply.getError());
                        logger.warn(err);
                        //TODO: schedule a job to clean up
                    } else {
                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        RevokePortForwardingRuleRsp ret = re.toResponse(RevokePortForwardingRuleRsp.class);
                        if (ret.isSuccess()) {
                            String info = String.format("successfully revoke port forwarding rules: %s", JSONObjectUtil.toJsonString(to));
                            logger.debug(info);
                        } else {
                            String err = String.format("failed to revoke port forwarding rules %, because %s", JSONObjectUtil.toJsonString(to), ret.getError());
                            logger.warn(err);
                            //TODO: schedule a job to clean up
                        }
                    }

                    chain.rollback();
                }
            });
        } else {
            chain.rollback();
        }
    }
}
