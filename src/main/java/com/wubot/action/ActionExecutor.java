package com.wubot.action;

import com.wubot.discovery.DiscoveryCollector;
import com.wubot.network.PacketSender;
import com.wubot.world.BoxEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes actions by sending packets through PacketSender.
 */
public class ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private final PacketSender sender;
    private final DiscoveryCollector discovery;

    public ActionExecutor(PacketSender sender, DiscoveryCollector discovery) {
        this.sender = sender;
        this.discovery = discovery;
    }

    /**
     * Execute an action.
     *
     * @param action the action to execute
     */
    public void execute(Action action) {
        if (action instanceof Action.Move m) {
            executeMove(m);
        } else if (action instanceof Action.Lock l) {
            executeLock(l);
        } else if (action instanceof Action.Attack) {
            executeAttack();
        } else if (action instanceof Action.StopAttack) {
            executeStopAttack();
        } else if (action instanceof Action.Collect c) {
            executeCollect(c);
        } else if (action instanceof Action.UseTeleport t) {
            executeTeleport(t);
        } else if (action instanceof Action.SwitchConfig sc) {
            executeSwitchConfig(sc);
        } else if (action instanceof Action.SwitchShip ss) {
            executeSwitchShip(ss);
        } else if (action instanceof Action.Wait w) {
            executeWait(w);
        }
    }

    private void executeMove(Action.Move action) {
        log.debug("Executing MOVE to ({}, {})", action.x(), action.y());
        sender.move(action.x(), action.y());
    }

    private void executeLock(Action.Lock action) {
        log.debug("Executing LOCK target {}", action.targetId());
        sender.lock(action.targetId());
    }

    private void executeAttack() {
        log.debug("Executing ATTACK");
        sender.attack();
    }

    private void executeStopAttack() {
        log.debug("Executing STOP_ATTACK");
        sender.stopAttack();
    }

    private void executeCollect(Action.Collect action) {
        // Move to box position with Y_OFFSET, then send collect
        // Note: The actual timing/sequencing should be handled by Brain
        // This just sends the collect packet - Brain should have already moved us there
        log.debug("Executing COLLECT box {} at ({}, {})",
                action.boxId(), action.boxX(), action.boxY());
        sender.collect(action.boxId());
    }

    private void executeTeleport(Action.UseTeleport action) {
        log.debug("Executing TELEPORT via portal {}", action.portalId());
        // Notify discovery BEFORE teleport for connection tracking
        discovery.onBeforeTeleport(action.portalId());
        sender.teleport(action.portalId());
    }

    private void executeSwitchConfig(Action.SwitchConfig action) {
        log.debug("Executing SWITCH_CONFIG to {}", action.configId());
        sender.switchConfig(action.configId());
    }

    private void executeSwitchShip(Action.SwitchShip action) {
        log.debug("Executing SWITCH_SHIP to hangar {}", action.shipId());
        sender.switchShip(action.shipId());
    }

    private void executeWait(Action.Wait action) {
        log.debug("Executing WAIT for {} ms", action.durationMs());
        try {
            Thread.sleep(action.durationMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wait interrupted");
        }
    }
}
