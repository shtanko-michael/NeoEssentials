package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.i18n.CustomLanguageManager;
import com.zerog.neoessentials.i18n.CustomLanguageManager.LanguageFileInfo;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Commands for managing custom language files and translations
 */
public class LanguageCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("language")
                .requires(source -> source.hasPermission(4)) // Op level 4
                .then(Commands.literal("list")
                    .executes(LanguageCommand::listLanguages))
                .then(Commands.literal("reload")
                    .executes(LanguageCommand::reloadLanguages))
                .then(Commands.literal("stats")
                    .executes(LanguageCommand::showStats))
                .then(Commands.literal("template")
                    .then(Commands.argument("languageCode", StringArgumentType.word())
                        .executes(LanguageCommand::generateTemplate)))
                .then(Commands.literal("exportmissing")
                    .executes(LanguageCommand::exportMissingKeys))
                .then(Commands.literal("clearmissing")
                    .executes(LanguageCommand::clearMissingKeys))
                .then(Commands.literal("info")
                    .executes(LanguageCommand::showInfo))
        );

        LOGGER.info("Language command registered");
    }

    /**
     * List all available custom languages
     */
    private static int listLanguages(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CustomLanguageManager manager = CustomLanguageManager.getInstance();

        List<LanguageFileInfo> languages = manager.getCustomLanguages();

        if (languages.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.language.list.none"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.list.add_hint"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.list.template_hint"), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.list.header", languages.size()), false);

            for (LanguageFileInfo lang : languages) {
                String info = String.format("  §e%s §7- §f%s §7(§f%s§7) §7by §f%s §7v%s",
                    lang.getLanguageCode(),
                    lang.getNativeName(),
                    lang.getEnglishName(),
                    lang.getAuthor(),
                    lang.getVersion()
                );
                source.sendSuccess(() -> MessageUtil.component(info), false);
            }
        }

        return 1;
    }

    /**
     * Reload all custom language files
     */
    private static int reloadLanguages(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        try {
            CustomLanguageManager.getInstance().reload();
            int count = CustomLanguageManager.getInstance().getCustomLanguages().size();

            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.reload.success", count), true);
            LOGGER.info("Custom languages reloaded by {}", source.getTextName());
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.language.reload.failed", e.getMessage()));
            LOGGER.error("Failed to reload languages", e);
        }

        return 1;
    }

    /**
     * Show statistics about translations
     */
    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Map<String, Object> stats = CustomLanguageManager.getInstance().getStatistics();

        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.stats.header"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.stats.loaded", stats.get("customLanguagesLoaded")), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.stats.missing_tracked", stats.get("missingKeysTracked")), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.stats.custom_dir", stats.get("customLanguageDirectory")), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.stats.template_dir", stats.get("templateDirectory")), false);

        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) stats.get("languageCodes");
        if (!codes.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.stats.available", String.join(", ", codes)), false);
        }

        return 1;
    }

    /**
     * Generate a translation template for a specific language
     */
    private static int generateTemplate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String languageCode = StringArgumentType.getString(ctx, "languageCode");

        try {
            String fileName = languageCode + "_template.json";
            CustomLanguageManager.getInstance().generateTemplate(
                languageCode,
                Paths.get("neoessentials", "languages", "templates", fileName)
            );

            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.template.success", languageCode), true);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.template.saved", fileName), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.template.instructions"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.template.step1"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.template.step2", languageCode), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.template.step3"), false);

            LOGGER.info("Generated language template for {} by {}", languageCode, source.getTextName());
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.language.template.failed", e.getMessage()));
            LOGGER.error("Failed to generate template for {}", languageCode, e);
        }

        return 1;
    }

    /**
     * Export missing translation keys to a file
     */
    private static int exportMissingKeys(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        try {
            int count = CustomLanguageManager.getInstance().getMissingKeys().size();

            if (count == 0) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.exportmissing.none"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.exportmissing.none_hint"), false);
                return 1;
            }

            String fileName = "missing_keys_" + System.currentTimeMillis() + ".json";
            CustomLanguageManager.getInstance().exportMissingKeys(
                Paths.get("neoessentials", "languages", "templates", fileName)
            );

            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.exportmissing.success", count), true);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.exportmissing.file", fileName), false);

            LOGGER.info("Exported {} missing keys by {}", count, source.getTextName());
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.language.exportmissing.failed", e.getMessage()));
            LOGGER.error("Failed to export missing keys", e);
        }

        return 1;
    }

    /**
     * Clear missing keys tracker
     */
    private static int clearMissingKeys(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int count = CustomLanguageManager.getInstance().getMissingKeys().size();

        CustomLanguageManager.getInstance().clearMissingKeys();

        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.clearmissing.success", count), true);

        return 1;
    }

    /**
     * Show general information about the language system
     */
    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.language.info.header"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.intro"), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.commands_header"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.cmd_list"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.cmd_reload"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.cmd_stats"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.cmd_template"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.cmd_exportmissing"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.cmd_clearmissing"), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.codes_header"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.codes_line1"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.codes_line2"), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.create_header"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.create_step1"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.create_step2"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.create_step3"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.language.info.create_step4"), false);

        return 1;
    }
}
