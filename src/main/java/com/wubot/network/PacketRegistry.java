package com.wubot.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Auth
import com.wubot.protocol.packets.auth.AuthRequestPacket;
import com.wubot.protocol.packets.auth.AuthAnswerPacket;
import com.wubot.protocol.packets.auth.SignUpRequestPacket;
import com.wubot.protocol.packets.auth.SignUpResponsePacket;
import com.wubot.protocol.packets.auth.AuthFractionRequestPacket;
import com.wubot.protocol.packets.auth.AuthFractionAnswerPacket;
import com.wubot.protocol.packets.auth.MapConnectRequestPacket;
import com.wubot.protocol.packets.auth.MapConnectAnswerPacket;

// Equipment
import com.wubot.protocol.packets.equip.Ammo;
import com.wubot.protocol.packets.equip.LaserAmmo;
import com.wubot.protocol.packets.equip.Drone;
import com.wubot.protocol.packets.equip.Ares;
import com.wubot.protocol.packets.equip.Nimbus;
import com.wubot.protocol.packets.equip.Equipment;
import com.wubot.protocol.packets.equip.LaserGun;
import com.wubot.protocol.packets.equip.SpeedGen;
import com.wubot.protocol.packets.equip.ShieldGen;
import com.wubot.protocol.packets.equip.Extension;
import com.wubot.protocol.packets.equip.ExtensionState;
import com.wubot.protocol.packets.equip.EnergyAmmo;
import com.wubot.protocol.packets.equip.RocketAmmo;
import com.wubot.protocol.packets.equip.DroneCover;
import com.wubot.protocol.packets.equip.ResourceInfo;
import com.wubot.protocol.packets.equip.EquipRequestPacket;
import com.wubot.protocol.packets.equip.EquipResponsePacket;
import com.wubot.protocol.packets.equip.EquipMoveRequestPacket;
import com.wubot.protocol.packets.equip.EquipMoveResponsePacket;
import com.wubot.protocol.packets.equip.SellItemRequestPacket;
import com.wubot.protocol.packets.equip.SellItemResponsePacket;
import com.wubot.protocol.packets.equip.EquipHangarActionRequest;
import com.wubot.protocol.packets.equip.EquipHangarActionResponse;

// Auction
import com.wubot.protocol.packets.auction.AuctionItemsRequestPacket;
import com.wubot.protocol.packets.auction.AuctionItemsResponsePacket;
import com.wubot.protocol.packets.auction.AuctionBidRequestPacket;
import com.wubot.protocol.packets.auction.AuctionBidResponsePacket;
import com.wubot.protocol.packets.auction.AuctionNetPacket;
import com.wubot.protocol.packets.auction.AuctionNotificationNetPacket;

// Clan
import com.wubot.protocol.packets.clan.ClanActionRequestPacket;
import com.wubot.protocol.packets.clan.ClanActionResponsePacket;
import com.wubot.protocol.packets.clan.ClanMemberInPacket;
import com.wubot.protocol.packets.clan.ClanInPacket;
import com.wubot.protocol.packets.clan.ClanDiplomacyInPacket;

// Shop
import com.wubot.protocol.packets.shop.ShopItemsRequestPacket;
import com.wubot.protocol.packets.shop.ShopItemsResponsePacket;
import com.wubot.protocol.packets.shop.ShopBuyRequestPacket;
import com.wubot.protocol.packets.shop.ShopBuyResponsePacket;

// Stats
import com.wubot.protocol.packets.stats.StatsRequest;
import com.wubot.protocol.packets.stats.GeneralStatsResponse;
import com.wubot.protocol.packets.stats.ScoreStatsResponse;
import com.wubot.protocol.packets.stats.ClanStatsResponse;
import com.wubot.protocol.packets.stats.OnlineStatsResponse;
import com.wubot.protocol.packets.stats.RegularStatsResponse;

// Chat
import com.wubot.protocol.packets.chat.ChatMessageRequest;
import com.wubot.protocol.packets.chat.ChatMessageResponse;
import com.wubot.protocol.packets.chat.ChatAvailableRoomsRequestPacket;
import com.wubot.protocol.packets.chat.ChatNetPacket;

// Missions
import com.wubot.protocol.packets.missions.MissionsActionRequestPacket;
import com.wubot.protocol.packets.missions.MissionsActionResponsePacket;

// Quests
import com.wubot.protocol.packets.quests.missions.QuestsActionRequestPacket;
import com.wubot.protocol.packets.quests.missions.QuestsActionResponsePacket;

// Squads
import com.wubot.protocol.packets.squads.SquadsNetPacket;

// API
import com.wubot.protocol.api.ApiRequestPacket;
import com.wubot.protocol.api.ApiResponseNetStatus;
import com.wubot.protocol.api.ApiResponsePacket;
import com.wubot.protocol.api.ApiNotification;

// Main packets
import com.wubot.protocol.packets.ShipInPacket;
import com.wubot.protocol.packets.GameStateResponsePacket;
import com.wubot.protocol.packets.UserActionsPacket;
import com.wubot.protocol.packets.EventResponsePacket;
import com.wubot.protocol.packets.MapInfoPacket;
import com.wubot.protocol.packets.MessageResponsePacket;
import com.wubot.protocol.packets.TeleportRequestPacket;
import com.wubot.protocol.packets.TeleportResponsePacket;
import com.wubot.protocol.packets.RepairRequestPacket;
import com.wubot.protocol.packets.RepairResponsePacket;
import com.wubot.protocol.packets.RepairCostRequestPacket;
import com.wubot.protocol.packets.RepairCostResponsePacket;
import com.wubot.protocol.packets.GameEvent;
import com.wubot.protocol.packets.SpaceballEventInfo;
import com.wubot.protocol.packets.FractionChangeRequest;
import com.wubot.protocol.packets.FractionChangeResponse;
import com.wubot.protocol.packets.CollectableCollectRequest;
import com.wubot.protocol.packets.CollectableInPacket;
import com.wubot.protocol.packets.RocketShotRequest;
import com.wubot.protocol.packets.RocketSwitchRequest;
import com.wubot.protocol.packets.AutoRocketRequest;
import com.wubot.protocol.packets.ChangeCredentialsRequest;
import com.wubot.protocol.packets.ChangeCredentialsResponse;
import com.wubot.protocol.packets.HangarInPacket;
import com.wubot.protocol.packets.UserInfoResponsePacket;
import com.wubot.protocol.packets.ChangedParameter;
import com.wubot.protocol.packets.ResourcesPack;
import com.wubot.protocol.packets.equip.ResourcesActionRequestPacket;
import com.wubot.protocol.packets.equip.ResourcesInfoResponsePacket;
import com.wubot.protocol.packets.equip.ResourcesTradeInfoResponsePacket;
import com.wubot.protocol.packets.ConvoyEventInfo;
import com.wubot.protocol.packets.RewardItem;
import com.wubot.protocol.packets.Reward;
import com.wubot.protocol.packets.MapEvent;
import com.wubot.protocol.packets.ClientOnPausePacket;
import com.wubot.protocol.packets.ClientOnResumePacket;
import com.wubot.protocol.packets.ClientInfoNetPacket;

/**
 * Registers all packets with Kryo serializer.
 *
 * ORDER IS CRITICAL - must match client exactly!
 * Source: Decompiled jr.class + ClientPackets.class
 */
public class PacketRegistry {

    private static final Logger log = LoggerFactory.getLogger(PacketRegistry.class);

    /**
     * Register all packets with Kryo in the EXACT order as the client.
     * Any deviation will cause deserialization errors!
     */
    public static void register(Kryo kryo) {
        log.info("Registering packets with Kryo (exact client order)...");

        // === 1. Базовые типы ===
        kryo.register(Object.class);
        kryo.register(Object[].class);
        kryo.register(int.class);
        kryo.register(int[].class);
        kryo.register(String.class);
        kryo.register(String[].class);

        // === 2. Авторизация ===
        kryo.register(AuthRequestPacket.class);
        kryo.register(AuthAnswerPacket.class);
        kryo.register(SignUpRequestPacket.class);
        kryo.register(SignUpResponsePacket.class);
        kryo.register(AuthFractionRequestPacket.class);
        kryo.register(AuthFractionAnswerPacket.class);
        kryo.register(MapConnectRequestPacket.class);
        kryo.register(MapConnectAnswerPacket.class);

        // === 3. Оборудование (Equipment) ===
        kryo.register(Ammo.class);
        kryo.register(LaserAmmo.class);
        kryo.register(Ammo[].class);
        kryo.register(Drone.class);
        kryo.register(Drone[].class);
        kryo.register(Ares.class);
        kryo.register(Nimbus.class);
        kryo.register(Equipment.class);
        kryo.register(Equipment[].class);
        kryo.register(LaserGun.class);
        kryo.register(SpeedGen.class);
        kryo.register(ShieldGen.class);

        // === 4. Корабли ===
        kryo.register(ShipInPacket.class);
        kryo.register(ShipInPacket[].class);

        // === 5. Equip пакеты ===
        kryo.register(EquipRequestPacket.class);
        kryo.register(EquipResponsePacket.class);
        kryo.register(EquipMoveRequestPacket.class);
        kryo.register(EquipMoveResponsePacket.class);
        kryo.register(SellItemRequestPacket.class);
        kryo.register(SellItemResponsePacket.class);

        // === 6. Аукцион ===
        kryo.register(AuctionItemsRequestPacket.class);
        kryo.register(AuctionItemsResponsePacket.class);
        kryo.register(AuctionBidRequestPacket.class);
        kryo.register(AuctionBidResponsePacket.class);

        // === 7. Кланы ===
        kryo.register(ClanActionRequestPacket.class);
        kryo.register(ClanActionResponsePacket.class);
        kryo.register(ClanMemberInPacket.class);
        kryo.register(ClanMemberInPacket[].class);
        kryo.register(ClanInPacket.class);
        kryo.register(ClanInPacket[].class);

        // === 8. Магазин ===
        kryo.register(ShopItemsRequestPacket.class);
        kryo.register(ShopItemsResponsePacket.class);
        kryo.register(ShopBuyRequestPacket.class);
        kryo.register(ShopBuyResponsePacket.class);

        // === 9. Статистика ===
        kryo.register(StatsRequest.class);
        kryo.register(GeneralStatsResponse.class);
        kryo.register(ScoreStatsResponse.class);
        kryo.register(ClanStatsResponse.class);
        kryo.register(OnlineStatsResponse.class);

        // === 10. Игровое состояние ===
        kryo.register(GameStateResponsePacket.class);
        kryo.register(UserActionsPacket.class);
        kryo.register(UserActionsPacket.UserAction.class);
        kryo.register(UserActionsPacket.UserAction[].class);
        kryo.register(GameStateResponsePacket.ShipInResponse.class);
        kryo.register(GameStateResponsePacket.ShipInResponse[].class);

        // === 11. Чат ===
        kryo.register(ChatMessageRequest.class);
        kryo.register(ChatMessageResponse.class);

        // === 12. События и карта ===
        kryo.register(EventResponsePacket.class);
        kryo.register(MapInfoPacket.class);
        kryo.register(MapInfoPacket.TPort.class);
        kryo.register(MapInfoPacket.TPort[].class);
        kryo.register(MessageResponsePacket.class);

        // === 13. Телепортация ===
        kryo.register(TeleportRequestPacket.class);
        kryo.register(TeleportResponsePacket.class);

        // === 14. Ремонт ===
        kryo.register(RepairRequestPacket.class);
        kryo.register(RepairResponsePacket.class);

        // === 15. Расширения и боеприпасы ===
        kryo.register(Extension.class);
        kryo.register(ExtensionState.class);
        kryo.register(EnergyAmmo.class);
        kryo.register(RocketAmmo.class);

        // === 16. События игры ===
        kryo.register(GameEvent.class);
        kryo.register(GameEvent[].class);
        kryo.register(SpaceballEventInfo.class);

        // === 17. Фракции ===
        kryo.register(FractionChangeRequest.class);
        kryo.register(FractionChangeResponse.class);

        // === 18. Ремонт (стоимость) ===
        kryo.register(RepairCostRequestPacket.class);
        kryo.register(RepairCostResponsePacket.class);

        // === 19. Массивы и сбор ===
        kryo.register(int[][].class);
        kryo.register(CollectableCollectRequest.class);

        // === 20. Ракеты ===
        kryo.register(RocketShotRequest.class);
        kryo.register(RocketSwitchRequest.class);
        kryo.register(AutoRocketRequest.class);

        // === 21. Дипломатия кланов ===
        kryo.register(ClanDiplomacyInPacket.class);
        kryo.register(ClanDiplomacyInPacket[].class);

        // === 22. Смена учётных данных ===
        kryo.register(ChangeCredentialsRequest.class);
        kryo.register(ChangeCredentialsResponse.class);

        // === 23. Чат (комнаты) ===
        kryo.register(ChatAvailableRoomsRequestPacket.class);

        // === 24. Миссии ===
        kryo.register(MissionsActionRequestPacket.class);
        kryo.register(MissionsActionResponsePacket.class);

        // === 25. Дроны (покрытие) ===
        kryo.register(DroneCover.class);

        // === 26. Ангар ===
        kryo.register(HangarInPacket.class);
        kryo.register(HangarInPacket[].class);
        kryo.register(EquipHangarActionRequest.class);
        kryo.register(EquipHangarActionResponse.class);

        // === 27. Пользователь ===
        kryo.register(UserInfoResponsePacket.class);
        kryo.register(ChangedParameter.class);
        kryo.register(ChangedParameter[].class);

        // === 28. Собираемые предметы ===
        kryo.register(CollectableInPacket.class);
        kryo.register(CollectableInPacket[].class);

        // === 29. Ресурсы ===
        kryo.register(ResourcesPack.class);
        kryo.register(ResourcesActionRequestPacket.class);
        kryo.register(ResourcesInfoResponsePacket.class);
        kryo.register(ResourcesTradeInfoResponsePacket.class);
        kryo.register(ResourceInfo[].class);
        kryo.register(ResourceInfo.class);
        kryo.register(ResourcesInfoResponsePacket.EnrichmentInfo[].class);
        kryo.register(ResourcesInfoResponsePacket.EnrichmentInfo.class);

        // === 30. Конвой ===
        kryo.register(ConvoyEventInfo.class);

        // === 31. Квесты ===
        kryo.register(QuestsActionRequestPacket.class);
        kryo.register(QuestsActionResponsePacket.class);

        // === 32. Награды ===
        kryo.register(Object[][].class);
        kryo.register(RewardItem.class);
        kryo.register(RewardItem[].class);
        kryo.register(Reward.class);

        // === 33. Статистика (регулярная) ===
        kryo.register(RegularStatsResponse.class);

        // === 34. События на карте ===
        kryo.register(MapEvent.class);
        kryo.register(MapEvent[].class);

        // === 35. ClientPackets.getPacketsToRegister() ===
        kryo.register(SquadsNetPacket.class);
        kryo.register(ChatNetPacket.class);
        kryo.register(ClientOnPausePacket.class);
        kryo.register(ClientOnResumePacket.class);
        kryo.register(AuctionNetPacket.class);
        kryo.register(AuctionNotificationNetPacket.class);
        kryo.register(ClientInfoNetPacket.class);
        kryo.register(ApiRequestPacket.class);
        kryo.register(ApiResponseNetStatus.class);
        kryo.register(ApiResponsePacket.class);
        kryo.register(ApiNotification.class);

        log.info("Packet registration completed (120+ classes in exact client order)");
    }

    /**
     * Register all packets with KryoNet EndPoint.
     */
    public static void registerKryoClasses(EndPoint endPoint) {
        register(endPoint.getKryo());
    }
}
