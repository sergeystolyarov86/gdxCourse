package com.mygdx.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {

	/**
	 * На ноутбуках с двумя видеокартами Windows по умолчанию отдаёт Java встроенной Intel,
	 * а её драйвер даёт белое окно и зависание. Эта переменная окружения заставляет
	 * NVIDIA Optimus запустить процесс на дискретной видеокарте. Читается только при старте
	 * процесса, поэтому лаунчер перезапускает сам себя с ней.
	 */
	private static final String GPU_ENV = "SHIM_MCCOMPAT";
	private static final String GPU_ENV_VALUE = "0x800000001";

	public static void main (String[] arg) {
		if (relaunchOnDiscreteGpu(arg)) return;
		exitWhenParentDies();

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Sponge Bob");
		config.setWindowedMode(800, 526);
		config.setForegroundFPS(60);

		new Lwjgl3Application(new Main(), config);
	}

	/** @return {@code true}, если игра отработала в дочернем процессе и текущий нужно завершить */
	private static boolean relaunchOnDiscreteGpu(String[] args) {
		if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return false;
		if (System.getenv(GPU_ENV) != null) return false;

		List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		for (String jvmArg : jvmArgs) {
			if (jvmArg.contains("jdwp")) return false; // под отладчиком остаёмся в этом процессе
		}

		List<String> command = new ArrayList<>();
		command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		command.addAll(jvmArgs);
		command.add("-Dgame.relaunched=true");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add(DesktopLauncher.class.getName());
		for (String a : args) command.add(a);

		ProcessBuilder builder = new ProcessBuilder(command)
				.redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT); // stdin — pipe, см. exitWhenParentDies
		builder.environment().put(GPU_ENV, GPU_ENV_VALUE);

		try {
			final Process child = builder.start();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					child.destroy();
				}
			}));
			System.exit(child.waitFor());
			return true;
		} catch (IOException | InterruptedException e) {
			System.err.println("Не удалось перезапустить на дискретной видеокарте, запускаю как есть: " + e);
			return false;
		}
	}

	/**
	 * Дочерний процесс держит pipe на stdin от родителя. Если родителя убили
	 * (например, кнопкой Stop в IDE), pipe закрывается — закрываем и игру.
	 */
	private static void exitWhenParentDies() {
		if (!Boolean.getBoolean("game.relaunched")) return;
		Thread watcher = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (System.in.read() != -1) {
						// ждём закрытия pipe
					}
				} catch (IOException ignored) {
				}
				System.exit(0);
			}
		}, "parent-watcher");
		watcher.setDaemon(true);
		watcher.start();
	}
}
