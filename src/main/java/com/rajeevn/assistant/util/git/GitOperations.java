package com.rajeevn.assistant.util.git;

import com.rajeevn.assistant.KeyWord;
import com.rajeevn.assistant.Util;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.util.Optional;

public class GitOperations
{
	private static String repo;

	@KeyWord ("Clone repo ${url} into folder ${folder}")
	public static void cloneRepo(String url, String folder)
	{
		try
		{
			Util.execCommand("cmd.exe /c git clone " + url + " " + folder);
			repo = folder;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@KeyWord ("Sync branch ${branch} in ${projectFolder}")
	public static void syncMyBranches(String branch, String projectFolder) throws Exception
	{
		try
		{
			Util.execCommand("cmd.exe /c git-merge.bat from master to " + branch + " in " + projectFolder);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@KeyWord("switch to ${branch} branch")
	public static void switchBranch(String branch) throws Exception
	{
		switchBranch(branch, repo);
	}

	@KeyWord ("switch to ${branch} branch in repo ${repoPath}")
	public static void switchBranch(String branch, String repoPath) throws Exception
	{
		try (Git git = Git.open(new File(repoPath)))
		{
			Optional<Ref> branchRef = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().stream()
					.filter(ref -> ref.getName().contains(branch))
					.findFirst();
			if (branchRef.isPresent())
			{
				String branchName = branchRef.get().getName().substring("refs/remotes/origin/".length());
				boolean createBranch = git.getRepository().exactRef("refs/heads/" + branch) == null;
				git.checkout()
						.setCreateBranch(createBranch)
						.setName(branchName)
						.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
						.setStartPoint("origin/" + branchName)
						.call();
			}
			else
			{
				System.err.println("matching branch not found");
			}
		}
	}
}