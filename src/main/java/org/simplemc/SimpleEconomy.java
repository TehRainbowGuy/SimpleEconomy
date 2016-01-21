package org.simplemc;

import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.simplemc.commands.MoneyCommand;
import org.simplemc.database.DatabaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;
import java.util.jar.JarFile;

public class SimpleEconomy extends JavaPlugin
{
    private DatabaseManager databaseManager;
    HashMap<UUID, Account> accounts = new HashMap<>();
    YamlConfiguration languageConfig;

    @Override
    public void onEnable()
    {
        getLogger().info(getName() + " is loading...");
        saveDefaultConfig();
        try
        {
            //Copy over a temp version of the language file as you are not supposed to use inputstreams for this according to the spigot javadocs.
            File temp = File.createTempFile("lang", ".yml");
            temp.deleteOnExit();
            FileOutputStream fileOutputStream = new FileOutputStream(temp);
            IOUtils.copy(getClass().getResourceAsStream("/lang.yml"), fileOutputStream);
            languageConfig = YamlConfiguration.loadConfiguration(temp);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        databaseManager = new DatabaseManager(this);
        getCommand("money").setExecutor(new MoneyCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getLogger().info(getName() + " has finished loading!");
    }

    @Override
    public void onDisable()
    {
        accounts.values().forEach(Account::save);
        accounts.clear();
    }

    public DatabaseManager getDatabaseManager()
    {
        return databaseManager;
    }

    /**
     * Requests an account from the database.
     *
     * @param uuid UUID of the account
     * @return Account
     */
    public Account getAccount(UUID uuid)
    {
        if (accounts.containsKey(uuid))
        {
            return accounts.get(uuid);
        }

        ResultSet resultSet = getDatabaseManager().selectAccount(uuid);
        try
        {
            if (resultSet.next())
            {
                int resultId = resultSet.getInt("id");
                String resultUUID = resultSet.getString("uuid");
                double resultBalance = resultSet.getDouble("balance");
                Account account = new Account(resultId, UUID.fromString(resultUUID), resultBalance, this);
                accounts.put(UUID.fromString(resultUUID), account);
                return account;
            }

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return createAccount(uuid, getConfig().getInt("defaults.balance"));
    }

    /**
     * Creates an account in the database.
     *
     * @param uuid    The uuid of the player
     * @param balance The starting balance of the account.
     * @return The new account
     */
    public Account createAccount(UUID uuid, double balance)
    {
        int accountId = getDatabaseManager().createAccount(uuid, balance);
        if (accountId != -1)
        {
            Account account = new Account(accountId, uuid, balance, this);
            accounts.put(uuid, account);
            return account;
        }
        else
        {
            throw new RuntimeException("Failure to create account.");
        }
    }

    public String formatPhrase(String phraseId, Object... objects)
    {
        return String.format(languageConfig.getString("phrases." + phraseId), objects);
    }
}
