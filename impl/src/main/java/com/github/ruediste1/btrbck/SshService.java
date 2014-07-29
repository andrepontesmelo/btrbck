package com.github.ruediste1.btrbck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Singleton;

import com.github.ruediste1.btrbck.dom.RemoteRepository;
import com.github.ruediste1.btrbck.dom.SshTarget;

@Singleton
public class SshService {
	private ThreadLocal<Boolean> sudoRemoteBtrbck = new ThreadLocal<>();

	public void setSudoRemoteBtrbck(boolean value) {
		sudoRemoteBtrbck.set(value);
	}

	protected boolean sudoRemoteBtrbck() {
		return Boolean.TRUE.equals(sudoRemoteBtrbck.get());
	}

	private ThreadLocal<Boolean> sudoRemoteBtrfs = new ThreadLocal<>();

	public void setSudoRemoteBtrfs(boolean value) {
		sudoRemoteBtrfs.set(value);
	}

	protected boolean sudoRemoteBtrfs() {
		return Boolean.TRUE.equals(sudoRemoteBtrfs.get());
	}

	private final class SshConnectionimpl implements SshConnection {
		private final Process process;

		private SshConnectionimpl(Process process) {
			this.process = process;
		}

		@Override
		public OutputStream getOutputStream() {
			return process.getOutputStream();
		}

		@Override
		public InputStream getInputStream() {
			return process.getInputStream();
		}

		@Override
		public void close() throws Exception {
			process.getInputStream().close();
			process.waitFor();
		}
	}

	public interface SshConnection {
		InputStream getInputStream();

		OutputStream getOutputStream();

		void close() throws Exception;
	}

	private ProcessBuilder processBuilder(SshTarget target,
			List<String> commands) {
		// construct command
		LinkedList<String> list = new LinkedList<String>();
		list.add("ssh");

		// add keyfile
		if (target.getKeyFile() != null) {
			list.add("-i");
			list.add(target.getKeyFile().getAbsolutePath());
		}

		// add port
		if (target.getPort() != null) {
			list.add("-p");
			list.add(target.getPort().toString());
		}

		// add other parameters
		list.addAll(target.getParameters());

		// add host
		list.add(target.getHost());

		// add commands
		list.addAll(commands);

		return new ProcessBuilder().redirectError(Redirect.INHERIT).command(
				list);
	}

	public SshConnection sendSnapshots(RemoteRepository repo,
			String remoteStreamName) throws IOException {
		List<String> commands = new ArrayList<>();
		if (sudoRemoteBtrbck()) {
			commands.add("sudo");
		}
		commands.add("btrbck");
		commands.add("-r");
		commands.add(repo.location);
		if (sudoRemoteBtrfs()) {
			commands.add("-sudo");
		}
		commands.add(remoteStreamName);
		final Process process = processBuilder(repo.sshTarget, commands)
				.start();
		return new SshConnectionimpl(process);
	}

	public SshConnection receiveSnapshots(RemoteRepository repo,
			String remoteStreamName, boolean createRemoteIfNecessary)
			throws IOException {
		List<String> commands = new ArrayList<>();
		if (sudoRemoteBtrbck()) {
			commands.add("sudo");
		}
		commands.add("btrbck");
		if (createRemoteIfNecessary) {
			commands.add("-c");
		}
		commands.add("-r");
		commands.add(repo.location);
		if (sudoRemoteBtrfs()) {
			commands.add("-sudo");
		}
		commands.add("receiveSnapshots");
		commands.add(remoteStreamName);
		Process process = processBuilder(repo.sshTarget, commands).start();
		return new SshConnectionimpl(process);
	}
}