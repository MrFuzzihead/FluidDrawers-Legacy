package com.mrfuzzihead.fluiddrawers.block;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import com.jaquadro.minecraft.storagedrawers.item.ItemPersonalKey;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeCreative;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStatus;
import com.jaquadro.minecraft.storagedrawers.security.SecurityManager;
import com.mrfuzzihead.fluiddrawers.FluidDrawers;
import com.mrfuzzihead.fluiddrawers.FluidDrawersCreativeTab;
import com.mrfuzzihead.fluiddrawers.drawers.DrawerUpgradable;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.init.FdGuis;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;
import com.mrfuzzihead.fluiddrawers.util.BlockInteractionUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTank extends Block implements ITileEntityProvider, INetworked {

    @SideOnly(Side.CLIENT)
    private IIcon iconTank;

    @SideOnly(Side.CLIENT)
    private IIcon iconTankVending;

    @SideOnly(Side.CLIENT)
    private IIcon iconGlass;

    // Phase 11/10 overlays (were never rendered -- fixed now). These reuse StorageDrawers'
    // overlay textures (SD is a hard dependency so the textures are always stitched).
    @SideOnly(Side.CLIENT)
    private IIcon iconTape;
    @SideOnly(Side.CLIENT)
    private IIcon iconLock;
    @SideOnly(Side.CLIENT)
    private IIcon iconClaim;
    @SideOnly(Side.CLIENT)
    private IIcon iconClaimLock;
    @SideOnly(Side.CLIENT)
    private IIcon iconVoid;
    @SideOnly(Side.CLIENT)
    private IIcon[] iconTrim;

    // Storage-trim overlay names matching SD 1.7.10 BlockDrawers (indices 2-8 = upgrade metadata).
    private static final String[] STORAGE_OVERLAYS = { null, null, "iron", "gold", "obsidian", "diamond", "emerald",
        "ruby", "tanzanite" };

    public BlockTank() {
        this(FluidDrawers.MODID + ".tank", Material.iron);
    }

    /**
     * Protected constructor for subclasses (the framed tank variant). Lets the subclass choose its
     * own unlocalized name and material while sharing the common frame/glass behaviour.
     */
    protected BlockTank(String blockName, Material material) {
        super(material);
        this.isBlockContainer = true;
        this.setBlockName(blockName);
        this.setHardness(5.0F);
        this.setStepSound(soundTypeMetal);
        this.setCreativeTab(FluidDrawersCreativeTab.INSTANCE);
    }

    // Custom render type -> BlockTankRenderer (registered in ClientProxy). Returns 0 on the
    // dedicated server (tankRenderId is only allocated client-side), which is fine since the
    // server never renders. Ports the 1.12.2 getRenderType() override.
    @Override
    public int getRenderType() {
        return ModBlocks.tankRenderId;
    }

    // Ports the 1.12.2 func_149662_c / func_149686_d overrides. The hollow frame must NOT be
    // treated as a full opaque cube, otherwise neighboring faces behind the glass get culled and
    // you see a black box through the glass, and AO/lighting is computed for a solid cube.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // Render across both passes: metal frame in pass 0 (solid), glass interior in pass 1 (alpha).
    // canRenderInPass(true) for every pass lets the chunk renderer call renderWorldBlock in both,
    // and BlockTankRenderer dispatches on ForgeHooksClient.getWorldRenderPass().
    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return true;
    }

    // --- Light emission ---

    // A tank holding a luminous fluid (e.g. lava) emits that fluid's light level. Delegates to the
    // tile's stored fluid luminosity (unscaled by fill -- matches the OpenBlocks Tank reference;
    // 1.12.2 FluidDrawers does not override getLightValue). The Forge-added
    // getLightValue(IBlockAccess, x, y, z) is the world/lighting-engine entry point (Block:1497);
    // the rendering pipeline also feeds it into getLightBrightnessForSkyBlocks (Block:609/615).
    // TileTank.onStoredFluidChanged triggers a relight (func_147451_t) when the luminosity changes.
    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileTank ? ((TileTank) tile).getFluidLightLevel() : 0;
    }

    // --- ITileEntityProvider ---

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileTank();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileTank tile = (TileTank) world.getTileEntity(x, y, z);
        if (tile != null && !tile.isSealed()) {
            // Non-sealed break: drop installed upgrades on the ground (skip creative upgrades,
            // matching SD 1.7.10 BlockDrawers.breakBlock). Fluid is NOT preserved -- it is
            // destroyed (no "Tile" NBT written in getDrops). This implements the requirement that
            // a non-sealed break returns a clean-slate tank.
            for (int i = 0; i < TileTank.UPGRADE_SLOT_COUNT; i++) {
                ItemStack stack = tile.getUpgradeInventory()
                    .getStackInSlot(i);
                if (stack != null) {
                    if (stack.getItem() instanceof ItemUpgradeCreative) continue;
                    dropBlockAsItem(world, x, y, z, stack);
                }
            }
            world.func_147453_f(x, y, z, block);
        }
        super.breakBlock(world, x, y, z, block, meta);
        world.removeTileEntity(x, y, z);
    }

    // --- Phase 12: break/drop/harvest coordination ---
    // 1.7.10 survival harvest calls removeBlock (-> setBlockToAir -> breakBlock -> removeTE)
    // BEFORE harvestBlock -> getDrops, so getDrops would see a null TE. Deferring removal via
    // removedByPlayer(willHarvest) + harvestBlock (matching SD 1.7.10 BlockDrawers) keeps the
    // block + TE alive until getDrops has read the portable NBT, then cleans up.

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (willHarvest) return true;
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
        super.harvestBlock(world, player, x, y, z, meta);
        world.setBlockToAir(x, y, z);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        ItemStack dropStack = new ItemStack(Item.getItemFromBlock(this), 1, metadata);
        TileTank tile = (TileTank) world.getTileEntity(x, y, z);
        if (tile != null) {
            // Sealed tank -> preserve all contents (fluid, upgrades, key statuses, custom name)
            // in the item's "Tile" portable NBT. Non-sealed -> clean-slate item (fluid destroyed,
            // upgrades dropped separately in breakBlock). Implements the requirement: sealed break
            // preserves; non-sealed break destroys fluid + drops upgrades + clean-slate item.
            if (tile.isSealed()) {
                NBTTagCompound tiledata = new NBTTagCompound();
                tile.writeToPortableNBT(tiledata);
                NBTTagCompound data = dropStack.getTagCompound();
                if (data == null) data = new NBTTagCompound();
                data.setTag("Tile", tiledata);
                dropStack.setTagCompound(data);
            }
            // Custom name travels on the item's display tag (visible in inventory) regardless of
            // sealed state -- it is the tank's identity, not its contents. It is also inside the
            // "Tile" portable NBT for restoration on place when sealed.
            if (tile.hasCustomName()) {
                dropStack.setStackDisplayName(tile.getCustomName());
            }
        }
        drops.add(dropStack);
        return drops;
    }

    @Override
    public boolean onBlockEventReceived(World world, int x, int y, int z, int eventNum, int eventArg) {
        super.onBlockEventReceived(world, x, y, z, eventNum, eventArg);
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile != null && tile.receiveClientEvent(eventNum, eventArg);
    }

    // --- Interaction dispatcher ---
    //
    // Ordered per section 4 of the backport plan (settled, do not reorder):
    // 1. tile == null → false
    // 2. security first (Phase 11)
    // 3. held-item: ItemKey/tape → ItemUpgrade → ItemPersonalKey → fluid transfer
    // 4. empty hand + sneak: unseal → open GUI

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileTank tile = (TileTank) world.getTileEntity(x, y, z);
        if (tile == null) return false;

        // Security-first guard: non-owners are blocked before any interaction
        if (!SecurityManager.hasAccess(player.getGameProfile(), tile)) {
            return false;
        }

        // Capture the original held item ONCE.
        ItemStack heldItem = player.getHeldItem();

        if (heldItem != null) {
            // Held-item dispatch:
            // Tape: return false so ItemTape.onItemUse handles sealing
            if (heldItem.getItem() == ModItems.tape) {
                return false;
            }
            // Upgrade items: right-click a tank with an upgrade in hand to
            // auto-apply it, matching 1.7.10 StorageDrawers BlockDrawers behavior.
            if (DrawerUpgradable.isUpgradeItem(heldItem)) {
                if (!tile.addUpgradeInteractive(heldItem)) {
                    if (!world.isRemote && !(heldItem.getItem() instanceof ItemUpgradeStatus)) {
                        // Check if the failure was due to capacity constraints
                        // (canAddUpgrade returned false) vs all slots full.
                        String msgKey = tile.hasUpgradeSpace() ? "storagedrawers.msg.cannotAddUpgrade"
                            : "storagedrawers.msg.maxUpgrades";
                        player.addChatMessage(new ChatComponentTranslation(msgKey));
                    }
                    return false;
                }
                if (!world.isRemote) {
                    if (!player.capabilities.isCreativeMode) {
                        heldItem.stackSize--;
                        if (heldItem.stackSize <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                        }
                    }
                    world.markBlockForUpdate(x, y, z);
                }
                return true;
            }
            // Lock key: toggle lock attributes (SD 1.7.10 pattern: clear zero-amount
            // prototype on unlock so a different fluid can be inserted afterwards)
            if (heldItem.getItem() == ModItems.upgradeLock) {
                if (!world.isRemote) {
                    boolean locked = tile.getAttributes()
                        .isItemLocked(LockAttribute.LOCK_POPULATED);
                    // Unlock first so setStoredFluid(null) can clear the zero-amount
                    // prototype via the new branch in SimpleFluidDrawer
                    tile.getAttributes()
                        .setItemLocked(LockAttribute.LOCK_EMPTY, !locked);
                    tile.getAttributes()
                        .setItemLocked(LockAttribute.LOCK_POPULATED, !locked);
                    if (locked) {
                        FluidDrawer drawer = tile.getDrawerGroup()
                            .getFluidDrawer();
                        FluidStack stored = drawer.getStoredFluid();
                        if (stored != null && stored.amount <= 0) {
                            drawer.setStoredFluid(null);
                        }
                    }
                    world.markBlockForUpdate(x, y, z);
                }
                return true;
            }
            // Concealment (shroud) key: toggle the concealed attribute, which hides the stored
            // fluid from the in-world TESR (RenderTileTank.renderFluid early-returns when
            // isConcealed()). Matches SD 1.7.10 BlockDrawers: setIsShrouded(!isShrouded()).
            // markBlockForUpdate pushes the attribute change to the client via the description
            // packet so the TESR re-renders immediately (the fluid quads are a dynamic TESR,
            // but the client TE only learns of the toggle through the synced "Attributes" NBT).
            // Slot per section 4 dispatcher: after lock, before personal key (SD order:
            // lock → shroud → quantify → personal). Security-first guard already ran above.
            if (heldItem.getItem() == ModItems.shroudKey) {
                if (!world.isRemote) {
                    tile.getAttributes()
                        .setConcealed(
                            !tile.getAttributes()
                                .isConcealed());
                    world.markBlockForUpdate(x, y, z);
                }
                return true;
            }
            // Quantify key: toggle the showing-quantity attribute, which renders a floating
            // "fluid name / X mB" label on all four sides of the tank (RenderTileTank).
            // Matches SD 1.7.10 BlockDrawers: setIsQuantified(!isQuantified()). Slot per the
            // section 4 dispatcher: after shroud, before personal key (SD order: lock → shroud
            // → quantify → personal). markBlockForUpdate pushes the attribute change to the
            // client so the TESR label appears/disappears immediately. The fluid amount is
            // already synced via the vanilla description packet (the 1.12.2 custom sync
            // packets are not needed -- our unified portable NBT carries fluid + count + the
            // quant attribute together; Chameleon's data split that necessitated them is gone).
            if (heldItem.getItem() == ModItems.quantifyKey) {
                if (!world.isRemote) {
                    tile.getAttributes()
                        .setShowingQuantity(
                            !tile.getAttributes()
                                .isShowingQuantity());
                    world.markBlockForUpdate(x, y, z);
                }
                return true;
            }
            // Personal key: toggle ownership
            if (heldItem.getItem() instanceof ItemPersonalKey) {
                if (!world.isRemote) {
                    if (tile.getOwner() == null) {
                        tile.setOwner(player.getPersistentID());
                    } else if (SecurityManager.hasOwnership(player.getGameProfile(), tile)) {
                        tile.setOwner(null);
                    } else {
                        return false;
                    }
                }
                return true;
            }
            // Fluid transfer — gated by facing != UP and !sealed
            if (side != 1 && !tile.isSealed()) {
                if (BlockInteractionUtils
                    .transferFluid(tile, player, heldItem, ForgeDirection.getOrientation(side), true)) {
                    return true;
                }
            }
        } else if (player.isSneaking()) {
            // Empty hand + sneak:
            if (tile.isSealed()) {
                if (!world.isRemote) {
                    tile.setIsSealed(false);
                    world.markBlockForUpdate(x, y, z);
                }
                return true;
            }
            // Not sealed → open GUI
            if (StorageDrawers.config.cache.enableDrawerUI) {
                player.openGui(FluidDrawers.instance, FdGuis.GUI_TANK, world, x, y, z);
                return true;
            }
        }

        return false;
    }

    // --- Redstone (Phase 10) ---

    // Direct weak power on all sides when redstone upgrade is installed.
    // Strong power on UP only. This is NOT a comparator override — it's direct
    // power, matching FD 1.12.2 behavior.
    // Matches the 1.12.2 func_180656_a (isProvidingWeakPower) and func_176211_b
    // (isProvidingStrongPower) overrides in BlockTankBase.

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileTank) {
            TileTank tank = (TileTank) tile;
            return tank.hasLevelEmitter() ? tank.getRedstoneLevel() : 0;
        }
        return 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        // Strong power only on UP side (side 1 = bottom face = ForgeDirection.DOWN = meta 0)
        // ForgeDirection ordinals: DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
        // Block.isProvidingStrongPower side param matches ForgeDirection ordinal
        if (side == 1) { // UP = the bottom face is being queried for power going upward
            return isProvidingWeakPower(world, x, y, z, side);
        }
        return 0;
    }

    // --- icons ---

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        this.iconTank = reg.registerIcon("fluiddrawers:tank");
        this.iconTankVending = reg.registerIcon("fluiddrawers:tank_vending");
        // Vanilla glass block texture (1.12.2 model references "minecraft:blocks/glass").
        this.iconGlass = reg.registerIcon("minecraft:glass");
        // Overlay textures from StorageDrawers (SD is a hard dependency).
        this.iconTape = reg.registerIcon("storagedrawers:tape");
        this.iconLock = reg.registerIcon("storagedrawers:indicator/lock_icon");
        this.iconClaim = reg.registerIcon("storagedrawers:indicator/claim_icon");
        this.iconClaimLock = reg.registerIcon("storagedrawers:indicator/claim_lock_icon");
        this.iconVoid = reg.registerIcon("storagedrawers:indicator/void_icon");
        this.iconTrim = new IIcon[STORAGE_OVERLAYS.length];
        for (int i = 2; i < STORAGE_OVERLAYS.length; i++) {
            this.iconTrim[i] = reg.registerIcon("storagedrawers:overlay_" + STORAGE_OVERLAYS[i]);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return iconTank;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconTank() {
        return iconTank;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconTankVending() {
        return iconTankVending;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconGlass() {
        return iconGlass;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconTape() {
        return iconTape;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconLock() {
        return iconLock;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconClaim() {
        return iconClaim;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconClaimLock() {
        return iconClaimLock;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconVoid() {
        return iconVoid;
    }

    /**
     * Returns the storage-trim overlay icon for the given upgrade metadata level, or null if
     * there is no trim for that level.
     */
    @SideOnly(Side.CLIENT)
    public IIcon getIconTrim(int level) {
        if (level < 2 || level >= iconTrim.length) return null;
        return iconTrim[level];
    }
}
