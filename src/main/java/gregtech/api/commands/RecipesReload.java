package gregtech.api.commands;

import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.loaders.postload.GT_CraftingRecipeLoader;
import gregtech.loaders.postload.GT_MachineRecipeLoader;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipesReload extends CommandBase {

    public static final List<Class<? extends Runnable>> runLoaders = new ArrayList<>();

    public static final List<Class<? extends Runnable>> currRunLoaders = new ArrayList<>();

    public static final Map<String, Class> classMap = new HashMap<>();

    static {
        classMap.put("GT_CraftingRecipeLoader", GT_CraftingRecipeLoader.class);
        classMap.put("GT_MachineRecipeLoader", GT_MachineRecipeLoader.class);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public String getCommandName() {
        return "rr";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "Reload recipes /rr - reload all recipe classes";
    }

    @Override
    public void processCommand(ICommandSender ics, String[] args) {
        if (args.length == 0) {
            clearRecipeMaps();
            ics.addChatMessage(new ChatComponentText("Recipes Clear"));
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("clear")) {
                currRunLoaders.clear();
            }
            if (args[0].equalsIgnoreCase("refresh")) {
                try {
                    reloadRecipe();
                    ics.addChatMessage(new ChatComponentText("Successful"));
                } catch (IllegalAccessException | InstantiationException ignored) {
                    ics.addChatMessage(new ChatComponentText("Error"));
                }
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                Class cl = classMap.get(args[1]);
                if (cl == null) {
                    ics.addChatMessage(new ChatComponentText("Unknown class"));
                } else {
                    if (Runnable.class.isAssignableFrom(cl)) {
                        currRunLoaders.add(cl);
                        ics.addChatMessage((new ChatComponentText("added")));
                    }
                }
            }
        }
    }

    public void reloadRecipe() throws IllegalAccessException, InstantiationException {
        if (currRunLoaders.size() > 0) {
            for (Class<? extends Runnable> currRunLoader : currRunLoaders) {
                currRunLoader.newInstance().run();
            }
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender isc, String[] args) {
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                List<String> ret = new ArrayList<>();
                for (String str : classMap.keySet()) {
                    if (str.startsWith(args[1])) {
                        ret.add(str);
                    }
                }
                return ret;
            }
        }
        return null;
    }

    public void clearRecipeMaps() {
        for (GT_Recipe.GT_Recipe_Map tMap : GT_Recipe.GT_Recipe_Map.sMappings) {
            tMap.clear();
        }
        ArrayList<IRecipe> tList = (ArrayList<IRecipe>) CraftingManager.getInstance().getRecipeList();
        tList.clear();
    }
}
