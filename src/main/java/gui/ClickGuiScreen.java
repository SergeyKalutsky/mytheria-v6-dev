package gui;

import com.google.common.base.Suppliers;
import config.Config;
import config.GroupScrollState;
import core.ClientMain;
import core.ConfigManager;
import core.FriendManager;
import core.Localization;
import core.SoundManager;
import enums.SoundType;
import font.MSDFFont;
import gui.GuiDdntRender;
import gui.InteractiveComponent;
import gui.br;
import gui.bze;
import gui.gt;
import gui.hvg;
import gui.im;
import gui.it;
import gui.jec;
import gui.jh;
import gui.jsj;
import gui.mi;
import gui.mu;
import gui.oh;
import gui.otj;
import gui.q;
import gui.srr;
import gui.v;
import gui.ws;
import gui.xr;
import gui.yd;
import gui.yx;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import module.ClickGuiModule;
import module.Module;
import module.PanicModule;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import p.xsx;
import render.BuiltBlur;
import render.BuiltLine2d;
import render.BuiltLiquidGlass;
import render.BuiltRectangle;
import render.BuiltText;
import render.Size;
import render.TextCache;

public class ClickGuiScreen
extends class_437 {
    private int WW = 1180;
    private int pe = 660;
    private float Hu = 230.0f;
    private float SF = 76.0f;
    private float dM = 160.0f;
    private static final Supplier<MSDFFont> cv = Suppliers.memoize(() -> MSDFFont.g().b("b").c("b").e());
    private static final Supplier<MSDFFont> atv = Suppliers.memoize(() -> MSDFFont.g().b("a").c("a").e());
    private final ConfigManager dh;
    private final v od;
    private final srr ahP;
    private final jh Tm;
    private final bze axm;
    private final mu arG;
    private final im amo;
    private final jsj ahX;
    private final List<InteractiveComponent> u = new ArrayList<InteractiveComponent>();
    private gt bN;
    private mi Xw;
    private BuiltRectangle Sg;
    private BuiltLine2d auW;
    private BuiltLine2d Ei;
    private BuiltRectangle ra;
    private BuiltBlur yk;
    private int CC = -1;
    private int vv = -1;
    private boolean Wr = true;
    private float Vd;
    private float azW;
    private float cA;
    private float ayw = 0.0f;
    private long iR = 0L;
    private boolean Xp = false;
    private boolean ams = false;
    private int Wz = 0;
    private boolean ua = false;
    private final double[] PV = new double[2];
    private int rk = -1;
    private int wj = -1;

    public ClickGuiScreen() {
        super((class_2561)class_2561.method_43470((String)"GUI Screen"));
        this.dh = ClientMain.getInstance().getConfigManager();
        this.od = new v(atv.get());
        this.ahP = new srr();
        this.Tm = new jh();
        this.axm = new bze();
        this.arG = new mu();
        this.ahX = new jsj(cv);
        float f = (float)this.WW - this.Hu;
        float f1 = (float)this.pe - this.SF + 6.0f;
        oh oh2 = new oh(this.Hu, this.SF, f, f1);
        yx yx2 = new yx(this.Hu, this.SF, f, f1);
        yd yd2 = new yd(this.Hu, this.SF, f, f1);
        this.amo = new im(oh2, yx2, yd2);
        this.initializeUI();
        this.restoreGuiState();
        this.setupCallbacks();
    }

    private void setupCallbacks() {
        this.ahP.m(s2 -> {
            this.amo.a((String)s2);
            this.updateModuleGrid();
            this.saveGuiState();
            if (this.bN != null) {
                this.bN.s();
            }
            if (this.bN != null) {
                boolean flag = "Friends".equals(s2) || "Configs".equals(s2);
                this.bN.a(flag);
            }
            if (this.Xw != null) {
                if ("Friends".equals(s2)) {
                    this.Xw.a(ws.tg);
                    return;
                }
                if ("Configs".equals(s2)) {
                    this.Xw.a(ws.ahz);
                }
            }
        });
        this.ahP.n(() -> this.amo.p().O());
        this.Tm.g(() -> {
            if (this.ahP.l().equals("Favourites")) {
                this.updateModuleGrid();
            }
        });
        this.axm.c(() -> {
            if (this.ahP.l().equals("Friends")) {
                this.amo.q().a();
            }
        });
        this.arG.d(() -> {
            if (this.ahP.l().equals("Configs")) {
                this.amo.r().k();
            }
        });
        if (this.Xw != null) {
            this.Xw.s(s2 -> {
                String s1 = this.ahP.l();
                if ("Friends".equals(s1)) {
                    this.handleAddFriend((String)s2);
                } else if ("Configs".equals(s1)) {
                    this.handleAddConfig((String)s2);
                }
            });
        }
    }

    private void restoreGuiState() {
        if (this.dh == null) {
            return;
        }
        Config config = this.dh.x();
        if (config != null) {
            Map<String, Float> map1;
            Map<String, GroupScrollState> map;
            Map<String, Float> map2;
            String s2 = config.f();
            if (s2 != null && !s2.isEmpty()) {
                this.ahP.i(s2);
                this.amo.a(s2);
                if (this.bN != null) {
                    boolean flag = "Friends".equals(s2) || "Configs".equals(s2);
                    this.bN.b(flag, true);
                }
                if (this.Xw != null) {
                    if ("Friends".equals(s2)) {
                        this.Xw.b(ws.tg, true);
                        this.Xw.c(true);
                    } else if ("Configs".equals(s2)) {
                        this.Xw.b(ws.ahz, true);
                        this.Xw.c(true);
                    } else {
                        this.Xw.c(false);
                    }
                }
            }
            if ((map2 = config.g()) != null) {
                this.amo.p().L(map2);
            }
            if ((map = config.h()) != null && !map.isEmpty()) {
                this.amo.p().e(map);
            }
            if ((map1 = config.i()) != null && !map1.isEmpty()) {
                this.amo.p().N(map1);
            }
        }
    }

    private void saveGuiState() {
        if (this.dh == null) {
            return;
        }
        Config config = this.dh.x();
        PanicModule panicmodule = ClientMain.getInstance().getModuleManager().getModule(PanicModule.class);
        if (config != null && panicmodule != null && !panicmodule.c() && !ClientMain.getInstance().getConfigSyncManager().k()) {
            this.updateConfigWithGuiState(config);
            this.dh.w(config);
            ClientMain.getInstance().getConfigSyncManager().d(config);
        }
    }

    public void updateConfigWithGuiState(Config config) {
        if (config != null) {
            config.s(this.ahP.l());
            config.t(this.amo.p().K());
            config.u(this.amo.p().c());
            config.v(this.amo.p().M());
        }
    }

    private void initializeUI() {
        this.buildUIElements();
        this.buildStaticComponents();
        this.buildCategoryComponents();
        this.setupSearchCallback();
    }

    private void buildUIElements() {
        this.Sg = (BuiltRectangle)new br().a(this.WW, this.pe).a();
        this.auW = (BuiltLine2d)new otj().a(this.WW).b(1.0f).d().j(0.2f).a();
        this.Ei = (BuiltLine2d)new otj().a(this.pe).b(1.0f).setAQ().j(0.2f).a();
    }

    private void buildStaticComponents() {
        float f = this.Hu + 18.0f;
        float f1 = 18.0f;
        this.Xw = new mi(f, f1);
        this.Xw.c(false);
        this.u.add(this.Xw);
        List<InteractiveComponent> list = this.ahX.b();
        for (InteractiveComponent component : list) {
            if (component instanceof gt) {
                component.setPosition(this.Hu + 18.0f, 18.0f);
            } else {
                component.setPosition(18.0f, 14.0f);
            }
        }
        this.u.addAll(list);
        list.stream().filter(interactivecomponent -> interactivecomponent instanceof gt).findFirst().ifPresent(interactivecomponent -> {
            this.bN = (gt)interactivecomponent;
        });
        List<InteractiveComponent> list1 = this.ahX.a(() -> this.ahP.c("Favourites"), () -> this.ahP.c("Friends"), () -> this.ahP.c("Configs"));
        float right = (float)this.WW - 18.0f;
        for (InteractiveComponent component : list1) {
            right -= component.getWidth();
            component.setPosition(right, 18.0f);
            right -= 10.0f;
        }
        this.u.addAll(list1);
        float f2 = 47.0f;
        float f3 = 30.0f;
        float f4 = 18.0f;
        float f5 = (float)this.pe - f3 - 18.0f;
        this.u.add(new jec(f4, f5, f2, f3));
    }

    private void buildCategoryComponents() {
        for (q q2 : this.ahX.c(this.ahP::c)) {
            this.ahP.b(q2);
            this.u.add(q2);
        }
    }

    private void setupSearchCallback() {
        this.u.stream().filter(interactivecomponent -> interactivecomponent instanceof gt).findFirst().ifPresent(interactivecomponent -> {
            gt gt2 = (gt)interactivecomponent;
            gt2.u((s2, s1) -> {
                String s22 = this.ahP.l();
                if ("Friends".equals(s22)) {
                    this.amo.q().i((String)s2);
                } else if ("Configs".equals(s22)) {
                    this.amo.r().m((String)s2);
                } else {
                    this.amo.p().b((String)s1);
                }
            });
        });
    }

    private void updateModuleGrid() {
        List<Module> list = this.getModulesForCurrentCategory();
        this.amo.p().a(list, this.ahP.l());
    }

    private List<Module> getModulesForCurrentCategory() {
        String s2 = this.ahP.l();
        if ("Favourites".equals(s2)) {
            return this.Tm.f(this.ahP.h());
        }
        if ("Friends".equals(s2)) {
            return Collections.emptyList();
        }
        return "Configs".equals(s2) ? Collections.emptyList() : this.ahP.g(s2);
    }

    public List<Module> getAllModules() {
        return this.ahP.h();
    }

    public void toggleModuleFavourite(String s2) {
        this.Tm.c(s2);
    }

    public boolean isModuleFavourite(String s2) {
        return this.Tm.d(s2);
    }

    private void handleAddFriend(String s2) {
        FriendManager.getInstance().addFriend(s2);
        this.amo.q().a();
    }

    private void handleAddConfig(String s2) {
        this.amo.r().n(s2);
    }

    protected void method_25426() {
        super.method_25426();
        SoundManager.getInstance().setMuted(true);
        this.iR = 0L;
        this.ayw = 0.0f;
        this.Xp = false;
        this.ams = true;
        this.Wz = 2;
        SoundManager.getInstance().setMuted(false);
        SoundManager.getInstance().c(SoundType.GUI_OPEN);
        this.amo.p().setClickGui(this);
        this.updateModuleGrid();
    }

    public void method_25394(class_332 drawcontext, int i, int j, float f) {
        if (this.Wz > 0) {
            --this.Wz;
            if (this.Wz == 0) {
                this.iR = System.currentTimeMillis();
            }
        } else {
            BuiltLiquidGlass.c();
            this.calculateScaling();
            this.updateGuiAlpha();
            if (!(this.ayw < 0.01f)) {
                class_310 minecraftclient = class_310.method_1551();
                int k2 = minecraftclient.method_22683().method_4486();
                int l = minecraftclient.method_22683().method_4502();
                Matrix4f matrix4f = drawcontext.method_51448().method_23760().method_23761();
                this.renderBackgroundEffects(matrix4f, k2, l);
                this.enableScissor(minecraftclient);
                drawcontext.method_51448().method_22903();
                this.applyGuiTransform(drawcontext);
                GuiDdntRender.begin(drawcontext.method_51448());
                Matrix4f matrix4f1 = drawcontext.method_51448().method_23760().method_23761();
                double[] adouble = this.scaleMousePosition(i, j);
                this.amo.h(this.cA, this.Vd, this.azW);
                this.renderBackground(matrix4f1);
                this.renderComponents(matrix4f1, adouble[0], adouble[1], f);
                this.updateTooltipFromHover(adouble[0], adouble[1]);
                this.od.c();
                GuiDdntRender.end();
                drawcontext.method_51448().method_22909();
                GL11.glDisable((int)3089);
                this.renderTooltip(drawcontext.method_51448().method_23760().method_23761());
                super.method_25394(drawcontext, i, j, f);
            }
        }
    }

    private void renderBackgroundEffects(Matrix4f matrix4f, int i, int j) {
        if (!(this.ayw < 0.01f)) {
            float f1;
            boolean flag;
            ClickGuiModule clickguimodule = ClientMain.getInstance().getModuleManager().getModule(ClickGuiModule.class);
            boolean bl = flag = clickguimodule == null || clickguimodule.b().getValue();
            if (i != this.CC || j != this.vv || flag != this.Wr) {
                this.CC = i;
                this.vv = j;
                this.Wr = flag;
                this.ra = (BuiltRectangle)new br().a((float)i + 50.0f, (float)j + 50.0f).a();
                this.yk = flag ? (BuiltBlur)new hvg().a(new Size(i, j)).e(10.0f).a() : null;
            }
            float f = 0.3f * this.ayw;
            this.ra.a(matrix4f, -10.0f, -10.0f, f);
            float f2 = f1 = flag ? 10.0f * this.ayw : 0.0f;
            if (this.yk != null && f1 > 0.5f) {
                this.yk.a(matrix4f, 0.0f, 0.0f, 0.0f, f1);
            }
        }
    }

    private void renderTooltip(Matrix4f matrix4f) {
        float f = this.Vd + (float)this.WW / 2.0f * this.cA;
        float f1 = this.azW;
        this.od.f(matrix4f, f, f1, this.ayw, this.cA);
    }

    private void updateTooltipFromHover(double d0, double d1) {
        String s2 = this.findHoveredTooltip(d0, d1);
        if (s2 != null && !s2.isEmpty()) {
            this.od.a(s2);
        } else {
            this.od.b();
        }
    }

    private String findHoveredTooltip(double d0, double d1) {
        String s1;
        for (InteractiveComponent interactivecomponent : this.u) {
            String s2;
            if (!interactivecomponent.isVisible() || !interactivecomponent.isHovered(d0, d1) || (s2 = this.getComponentDescription()) == null || s2.isEmpty()) continue;
            return s2;
        }
        if (this.isMouseInModuleArea(d0, d1) && (s1 = this.amo.o(d0, d1)) != null && !s1.isEmpty()) {
            return s1;
        }
        return null;
    }

    private String getComponentDescription() {
        return null;
    }

    private void updateGuiAlpha() {
        if (this.ams && this.iR != 0L) {
            long i = System.currentTimeMillis() - this.iR;
            if (i < 0L) {
                i = 0L;
            }
            float f = Math.min(1.0f, (float)i / this.dM);
            float f1 = xsx.a(f);
            if (this.Xp) {
                this.ayw = 1.0f - f1;
                if (f >= 1.0f) {
                    this.Xp = false;
                    this.ams = false;
                    super.method_25419();
                    return;
                }
            } else {
                this.ayw = f1;
            }
        } else {
            this.ayw = 0.0f;
        }
    }

    private void applyGuiTransform(class_332 drawcontext) {
        drawcontext.method_51448().method_46416(this.Vd, this.azW, 0.0f);
        drawcontext.method_51448().method_22905(this.cA, this.cA, 1.0f);
    }

    private void renderBackground(Matrix4f matrix4f) {
        this.Sg.a(matrix4f, 0.0f, 0.0f, this.ayw);
        int alpha = Math.max(0, Math.min(255, (int)(this.ayw * 255.0f)));
        int panel = alpha << 24 | 0x0D0F12;
        int panelSoft = Math.max(0, Math.min(210, (int)(this.ayw * 210.0f))) << 24 | 0x11151A;
        int accent = Math.max(0, Math.min(220, (int)(this.ayw * 220.0f))) << 24 | 0x8B5CF6;
        int subtle = Math.max(0, Math.min(90, (int)(this.ayw * 90.0f))) << 24 | 0xFFFFFF;
        GuiDdntRender.drawRoundRect(matrix4f, 10.0f, 10.0f, (float)this.WW - 20.0f, (float)this.pe - 20.0f, 16.0f, panel);
        GuiDdntRender.drawRoundRect(matrix4f, 18.0f, 68.0f, this.Hu - 36.0f, (float)this.pe - 102.0f, 12.0f, panelSoft);
        GuiDdntRender.drawRoundRect(matrix4f, this.Hu + 10.0f, 68.0f, (float)this.WW - this.Hu - 28.0f, (float)this.pe - 86.0f, 12.0f, panelSoft);
        GuiDdntRender.drawRectGradient(matrix4f, this.Hu, 58.0f, (float)this.WW - this.Hu - 18.0f, 1.0f, accent, subtle, subtle, accent);
        GuiDdntRender.drawText(matrix4f, "Mytheria", 70.0f, 20.0f, 20.0f, alpha << 24 | 0xFFFFFF);
        GuiDdntRender.drawText(matrix4f, this.ahP.l(), this.Hu + 18.0f, 52.0f, 13.0f, Math.max(0, Math.min(160, alpha)) << 24 | 0xFFFFFF);
        this.auW.a(matrix4f, 0.0f, 64.0f, this.ayw * 0.55f);
        this.Ei.a(matrix4f, this.Hu - 12.0f, 0.0f, this.ayw * 0.55f);
    }

    private void renderComponents(Matrix4f matrix4f, double d0, double d1, float f) {
        this.renderStaticComponents(matrix4f, (int)d0, (int)d1, f);
        String s2 = this.ahP.l();
        it it2 = this.ahP.j();
        it2.f();
        if (this.amo.d()) {
            this.amo.e(matrix4f, this.Hu, this.SF, (int)d0, (int)d1, f, this.ayw, it2);
        }
        if (this.amo.b()) {
            this.amo.f(matrix4f, this.Hu, this.SF, (int)d0, (int)d1, f, this.ayw, it2);
            if (this.axm.a()) {
                this.renderEmptyText(matrix4f, Localization.a().c("Здесь будет список друзей"));
            }
        }
        if (this.amo.c()) {
            this.amo.g(matrix4f, this.Hu, this.SF, (int)d0, (int)d1, f, this.ayw, it2);
        }
        if ("Favourites".equals(s2) && this.Tm.e()) {
            this.renderEmptyText(matrix4f, Localization.a().c("Здесь пока нет избранных модулей"));
        }
    }

    private void renderEmptyText(Matrix4f matrix4f, String s2) {
        BuiltText builttext;
        if (s2 == null || s2.isEmpty()) {
            return;
        }
        MSDFFont msdffont = atv.get();
        float f = 15.0f;
        float f1 = msdffont != null ? msdffont.c(s2, f) : (float)s2.length() * 8.0f;
        float f2 = (float)this.WW - this.Hu;
        float f3 = (float)this.pe - this.SF;
        float f4 = this.Hu + (f2 - f1) * 0.5f;
        float f5 = this.SF + (f3 - f) * 0.5f;
        BuiltText builtText = builttext = msdffont != null ? TextCache.a(msdffont, s2, f, new Color(255, 255, 255, 128)) : null;
        if (builttext != null) {
            builttext.a(matrix4f, f4 - 25.0f, f5 - 25.0f, this.ayw);
        } else {
            GuiDdntRender.drawText(matrix4f, s2, f4 - 25.0f, f5 - 25.0f, f, -2130706433);
        }
    }

    private void renderStaticComponents(Matrix4f matrix4f, int i, int j, float f) {
        for (InteractiveComponent interactivecomponent : this.u) {
            if (!interactivecomponent.isVisible()) continue;
            interactivecomponent.render(matrix4f, interactivecomponent.getX(), interactivecomponent.getY(), i, j, f, this.ayw);
        }
    }

    public void method_25393() {
        super.method_25393();
        float f = 0.05f;
        xr.h(this.u, f);
        this.amo.p().tick(f);
        if (this.bN != null && this.Xw != null) {
            boolean flag;
            String s2 = this.ahP.l();
            boolean bl = flag = "Friends".equals(s2) || "Configs".equals(s2);
            if (flag) {
                if (!this.Xw.isVisible()) {
                    this.Xw.c(true);
                    this.Xw.e();
                    return;
                }
            } else {
                float f1 = this.bN.t();
                if (f1 >= 730.0f) {
                    this.Xw.d();
                }
                if (f1 >= 769.0f) {
                    this.Xw.c(false);
                }
            }
        }
    }

    public boolean method_25402(double d0, double d1, int i) {
        if (!this.isWithinBounds(d0, d1)) {
            return super.method_25402(d0, d1, i);
        }
        double[] adouble = this.scaleMousePosition(d0, d1);
        if (xr.a(this.u, adouble[0], adouble[1], i)) {
            this.ua = false;
            return true;
        }
        if (this.isMouseInModuleArea(adouble[0], adouble[1])) {
            if (this.amo.i(adouble[0], adouble[1], i)) {
                this.ua = false;
                return true;
            }
            if (i == 0) {
                this.ua = true;
                return true;
            }
        }
        this.ua = false;
        return super.method_25402(d0, d1, i);
    }

    public boolean method_25406(double d0, double d1, int i) {
        double[] adouble = this.scaleMousePosition(d0, d1);
        if (i == 0) {
            this.ua = false;
            this.u.forEach(interactivecomponent -> interactivecomponent.mouseReleased(adouble[0], adouble[1], i));
            this.amo.j(adouble[0], adouble[1], i);
            return true;
        }
        boolean flag = xr.b(this.u, adouble[0], adouble[1], i);
        return (flag |= this.amo.j(adouble[0], adouble[1], i)) || super.method_25406(d0, d1, i);
    }

    public boolean method_25403(double d0, double d1, int i, double d2, double d3) {
        double[] adouble = this.scaleMousePosition(d0, d1);
        double d4 = d2 / (double)this.cA;
        double d5 = d3 / (double)this.cA;
        if (this.ua && i == 0) {
            this.amo.p().G(-d5 * 20.0);
            return true;
        }
        boolean flag = xr.c(this.u, adouble[0], adouble[1], i, d4, d5);
        return (flag |= this.amo.k(adouble[0], adouble[1], i, d4, d5)) || super.method_25403(d0, d1, i, d2, d3);
    }

    public boolean method_25401(double d0, double d1, double d2, double d3) {
        double[] adouble = this.scaleMousePosition(d0, d1);
        return this.isMouseInModuleArea(adouble[0], adouble[1]) && this.amo.l(adouble[0], adouble[1], d2, d3) ? true : xr.d(this.u, adouble[0], adouble[1], d2, d3) || super.method_25401(d0, d1, d2, d3);
    }

    public boolean method_25404(int i, int j, int k2) {
        if (this.amo.m(i, j, k2)) {
            return true;
        }
        boolean flag = xr.e(this.u, i, j, k2);
        return (flag |= this.amo.p().keyPressed(i, j, k2)) || super.method_25404(i, j, k2);
    }

    public boolean method_16803(int i, int j, int k2) {
        boolean flag = xr.f(this.u, i, j, k2);
        return (flag |= this.amo.p().keyReleased(i, j, k2)) || super.method_16803(i, j, k2);
    }

    public boolean method_25400(char c0, int i) {
        if (this.amo.n(c0, i)) {
            return true;
        }
        boolean flag = xr.g(this.u, c0, i);
        return (flag |= this.amo.p().charTyped(c0, i)) || super.method_25400(c0, i);
    }

    private boolean isMouseInModuleArea(double d0, double d1) {
        return d0 >= (double)this.Hu && d0 <= (double)this.WW && d1 >= (double)this.SF && d1 <= (double)this.pe;
    }

    private boolean isWithinBounds(double d0, double d1) {
        double[] adouble = this.scaleMousePosition(d0, d1);
        return adouble[0] >= 0.0 && adouble[0] <= (double)this.WW && adouble[1] >= 0.0 && adouble[1] <= (double)this.pe;
    }

    private double[] scaleMousePosition(double d0, double d1) {
        this.PV[0] = (d0 - (double)this.Vd) / (double)this.cA;
        this.PV[1] = (d1 - (double)this.azW) / (double)this.cA;
        return this.PV;
    }

    private void calculateScaling() {
        class_310 minecraftclient = class_310.method_1551();
        int i = minecraftclient.method_22683().method_4480();
        int j = minecraftclient.method_22683().method_4507();
        if (i != this.rk || j != this.wj) {
            this.rk = i;
            this.wj = j;
            double d0 = minecraftclient.method_22683().method_4495();
            float f = (float)i / 1920.0f;
            float f1 = (float)this.WW * f;
            float f2 = (float)this.pe * f;
            this.Vd = (float)((double)((float)i - f1) / 2.0 / d0);
            this.azW = (float)((double)((float)j - f2) / 2.0 / d0);
            this.cA = (float)((double)f / d0);
        }
    }

    private void enableScissor(class_310 minecraftclient) {
        double d0 = minecraftclient.method_22683().method_4495();
        int i = (int)((double)this.Vd * d0);
        int j = (int)((double)minecraftclient.method_22683().method_4507() - (double)(this.azW + (float)this.pe * this.cA) * d0);
        int k2 = (int)((double)((float)this.WW * this.cA) * d0);
        int l = (int)((double)((float)this.pe * this.cA) * d0);
        GL11.glEnable((int)3089);
        GL11.glScissor((int)i, (int)j, (int)k2, (int)l);
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25419() {
        if (!this.Xp) {
            this.Xp = true;
            this.iR = System.currentTimeMillis();
            SoundManager.getInstance().c(SoundType.GUI_CLOSE);
            this.saveGuiState();
        }
    }

    public void method_25432() {
        this.ams = false;
        this.ayw = 0.0f;
        super.method_25432();
    }

    public void method_25420(class_332 drawcontext, int i, int j, float f) {
    }

    public bze getFriends() {
        return this.axm;
    }
}
