/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.argument;
import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.literal;

import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection.MethodInvoker;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.mixin.HelpCommandAccessor;
import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

@Environment(EnvType.CLIENT)
public final class ClientCommandInternals {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommandInternals.class);
	private static final String API_COMMAND_NAME;
	private static final String SHORT_API_COMMAND_NAME = "fcc";
	private static @Nullable CommandDispatcher<FabricClientCommandSource> activeDispatcher;
	private static final Supplier<Class<?>> CommandException = () -> Reflection.getClass("net.minecraft.class_2164");
	private static final Supplier<MethodInvoker> CommandException_getTextMessage =
			Reflection.getOptionalMethod(CommandException, () -> "method_9199", () -> MethodType.methodType(Text.class));
	static {
		API_COMMAND_NAME = Version.<String>newSwitch()
				.range("1.19.0", null, "fabric-command-api-v2:client")
				.range(null, "1.18.2", "fabric-command-api-v1:client")
				.get();
		activeDispatcher = Version.<CommandDispatcher<FabricClientCommandSource>>newSwitch()
				.range("1.19.0", null, () -> null)
				.range(null, "1.18.2", () -> new CommandDispatcher<>())
				.get();
	}

	public static void setActiveDispatcher(@Nullable CommandDispatcher<FabricClientCommandSource> dispatcher) {
		ClientCommandInternals.activeDispatcher = dispatcher;
	}

	public static @Nullable CommandDispatcher<FabricClientCommandSource> getActiveDispatcher() {
		return activeDispatcher;
	}

	/**
	 * Executes a client-sided command. Callers should ensure that this is only called
	 * on slash-prefixed messages and the slash needs to be removed before calling.
	 * (This is the same requirement as {@code ClientPlayerEntity#sendCommand}.)
	 *
	 * @param command the command with slash removed
	 * @return true if the command should not be sent to the server, false otherwise
	 */
	public static boolean executeCommand(String command) {
		MinecraftClient client = MinecraftClient.getInstance();

		// The interface is implemented on ClientCommandSource with a mixin.
		// noinspection ConstantConditions
		FabricClientCommandSource commandSource = (FabricClientCommandSource) client.getNetworkHandler().getCommandSource();

		MVMisc.getProfiler().push(command);

		try {
			// TODO: Check for server commands before executing.
			//   This requires parsing the command, checking if they match a server command
			//   and then executing the command with the parse results.
			activeDispatcher.execute(command, commandSource);
			return true;
		} catch (CommandSyntaxException e) {
			boolean ignored = isIgnoredException(e.getType());

			if (ignored) {
				LOGGER.debug("Syntax exception for client-sided command '{}'", command, e);
				return false;
			}

			LOGGER.warn("Syntax exception for client-sided command '{}'", command, e);
			commandSource.sendError(getErrorMessage(e));
			return true;
		} catch (RuntimeException e) {
			if (Version.<Boolean>newSwitch().range("1.20.3", null, false).range(null, "1.20.2", true).get() &&
					CommandException.get().isInstance(e)) {
				LOGGER.warn("Error while executing client-sided command '{}'", command, e);
				commandSource.sendError(CommandException_getTextMessage.get().invoke(e));
				return true;
			} else {
				LOGGER.warn("Error while executing client-sided command '{}'", command, e);
				commandSource.sendError(TextInst.of(e.getMessage()));
				return true;
			}
		} finally {
			MVMisc.getProfiler().pop();
		}
	}

	/**
	 * Tests whether a command syntax exception with the type
	 * should be ignored and the command sent to the server.
	 *
	 * @param type the exception type
	 * @return true if ignored, false otherwise
	 */
	public static boolean isIgnoredException(CommandExceptionType type) {
		BuiltInExceptionProvider builtins = CommandSyntaxException.BUILT_IN_EXCEPTIONS;

		// Only ignore unknown commands and node parse exceptions.
		// The argument-related dispatcher exceptions are not ignored because
		// they will only happen if the user enters a correct command.
		return type == builtins.dispatcherUnknownCommand() || type == builtins.dispatcherParseException();
	}

	// See ChatInputSuggestor.formatException. That cannot be used directly as it returns an OrderedText instead of a Text.
	public static Text getErrorMessage(CommandSyntaxException e) {
		Text msg = Texts.toText(e.getRawMessage());
		String context = e.getContext();
		if (context == null)
			return msg;
		return TextInst.translatable("command.context.parse_error", msg, e.getCursor(), context);
	}

	/**
	 * Runs final initialization tasks such as {@link CommandDispatcher#findAmbiguities(AmbiguityConsumer)}
	 * on the command dispatcher. Also registers a {@code /fcc help} command if there are other commands present.
	 */
	public static void finalizeInit() {
		if (!activeDispatcher.getRoot().getChildren().isEmpty()) {
			// Register an API command if there are other commands;
			// these helpers are not needed if there are no client commands
			LiteralArgumentBuilder<FabricClientCommandSource> help = literal("help");
			help.executes(ClientCommandInternals::executeRootHelp);
			help.then(argument("command", StringArgumentType.greedyString()).executes(ClientCommandInternals::executeArgumentHelp));

			CommandNode<FabricClientCommandSource> mainNode = activeDispatcher.register(literal(API_COMMAND_NAME).then(help));
			activeDispatcher.register(literal(SHORT_API_COMMAND_NAME).redirect(mainNode));
		}

		// noinspection CodeBlock2Expr
		activeDispatcher.findAmbiguities((parent, child, sibling, inputs) -> {
			LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", activeDispatcher.getPath(child), activeDispatcher.getPath(sibling), inputs);
		});
	}

	private static int executeRootHelp(CommandContext<FabricClientCommandSource> context) {
		return executeHelp(activeDispatcher.getRoot(), context);
	}

	private static int executeArgumentHelp(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
		ParseResults<FabricClientCommandSource> parseResults = activeDispatcher.parse(StringArgumentType.getString(context, "command"), context.getSource());
		List<ParsedCommandNode<FabricClientCommandSource>> nodes = parseResults.getContext().getNodes();

		if (nodes.isEmpty()) {
			throw HelpCommandAccessor.getFailedException().create();
		}

		return executeHelp(Iterables.getLast(nodes).getNode(), context);
	}

	private static int executeHelp(CommandNode<FabricClientCommandSource> startNode, CommandContext<FabricClientCommandSource> context) {
		Map<CommandNode<FabricClientCommandSource>, String> commands = activeDispatcher.getSmartUsage(startNode, context.getSource());

		for (String command : commands.values()) {
			context.getSource().sendFeedback(TextInst.literal("/" + command));
		}

		return commands.size();
	}

	public static void addCommands(CommandDispatcher<FabricClientCommandSource> target, FabricClientCommandSource source) {
		Map<CommandNode<FabricClientCommandSource>, CommandNode<FabricClientCommandSource>> originalToCopy = new HashMap<>();
		originalToCopy.put(activeDispatcher.getRoot(), target.getRoot());
		copyChildren(activeDispatcher.getRoot(), target.getRoot(), source, originalToCopy);
	}

	/**
	 * Copies the child commands from origin to target, filtered by {@code child.canUse(source)}.
	 * Mimics vanilla's CommandManager.makeTreeForSource.
	 *
	 * @param origin         the source command node
	 * @param target         the target command node
	 * @param source         the command source
	 * @param originalToCopy a mutable map from original command nodes to their copies, used for redirects;
	 *                       should contain a mapping from origin to target
	 */
	private static void copyChildren(
			CommandNode<FabricClientCommandSource> origin,
			CommandNode<FabricClientCommandSource> target,
			FabricClientCommandSource source,
			Map<CommandNode<FabricClientCommandSource>, CommandNode<FabricClientCommandSource>> originalToCopy
	) {
		for (CommandNode<FabricClientCommandSource> child : origin.getChildren()) {
			if (!child.canUse(source)) continue;

			ArgumentBuilder<FabricClientCommandSource, ?> builder = child.createBuilder();

			// Reset the unnecessary non-completion stuff from the builder
			builder.requires(s -> true); // This is checked with the if check above.

			if (builder.getCommand() != null) {
				builder.executes(context -> 0);
			}

			// Set up redirects
			if (builder.getRedirect() != null) {
				builder.redirect(originalToCopy.get(builder.getRedirect()));
			}

			CommandNode<FabricClientCommandSource> result = builder.build();
			originalToCopy.put(child, result);
			target.addChild(result);

			if (!child.getChildren().isEmpty()) {
				copyChildren(child, result, source, originalToCopy);
			}
		}
	}
}
