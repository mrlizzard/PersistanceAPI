/*
===============================================================
   _____                       ______
  / ___/____ _____ ___  ____ _/ ____/___ _____ ___  ___  _____
  \__ \/ __ `/ __ `__ \/ __ `/ / __/ __ `/ __ `__ \/ _ \/ ___/
 ___/ / /_/ / / / / / / /_/ / /_/ / /_/ / / / / / /  __(__  )
/____/\__,_/_/ /_/ /_/\__,_/\____/\__,_/_/ /_/ /_/\___/____/

===============================================================
  Persistance API
  Copyright (c) for SamaGames, all right reserved
  By MisterSatch, January 2016
===============================================================
*/

package net.samagames.persistanceapi.datamanager.aggregationmanager.statistics;

import net.samagames.persistanceapi.beans.players.PlayerBean;
import net.samagames.persistanceapi.beans.statistics.DimensionStatisticsBean;
import net.samagames.persistanceapi.beans.statistics.LeaderboardBean;
import net.samagames.persistanceapi.utils.Transcoder;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class DimensionStatisticsManager
{
    // Defines
    Connection connection = null;
    Statement statement = null;
    ResultSet resultset = null;
    DimensionStatisticsBean dimensionStats = null;

    // Get Dimension player statistics
    public DimensionStatisticsBean getDimensionStatistics(PlayerBean player, DataSource dataSource)
    {
        try
        {
            // Set connection
            connection = dataSource.getConnection();
            statement = connection.createStatement();

            // Query construction
            String sql = "";
            sql += "select (HEX(uuid)) as uuid, deaths, kills, played_games, wins, creation_date, update_date, played_time from dimensions_stats";
            sql += " where uuid=(UNHEX('"+ Transcoder.Encode(player.getUuid().toString())+"'))";

            // Execute the query
            resultset = statement.executeQuery(sql);

            // Manage the result in a bean
            if(resultset.next())
            {
                // There's a result
                String playerUuid = Transcoder.Decode(resultset.getString("uuid"));
                UUID uuid = UUID.fromString(playerUuid);
                int deaths = resultset.getInt("deaths");
                int kills = resultset.getInt("kills");
                int playedGames = resultset.getInt("played_games");
                int wins = resultset.getInt("wins");
                Timestamp creationDate = resultset.getTimestamp("creation_date");
                Timestamp updateDate = resultset.getTimestamp("update_date");
                long playedTime = resultset.getLong("played_time");
                dimensionStats = new DimensionStatisticsBean(uuid, deaths, kills, playedGames, wins, creationDate, updateDate, playedTime);
            }
            else
            {
                // If there no dimension stats int the database
                return null;
            }
        }
        catch(SQLException exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            // Close the query environment in order to prevent leaks
            close();
        }
        return dimensionStats;
    }

    // Update Dimension player statistics
    public void updateDimensionStatistics(PlayerBean player, DimensionStatisticsBean dimensionStats, DataSource dataSource)
    {
        try
        {
            // Check if a record exists
            if (this.getDimensionStatistics(player, dataSource) == null)
            {
                // Set connection
                connection = dataSource.getConnection();
                statement = connection.createStatement();

                // Query construction for create
                String sql = "";
                sql += "insert into dimensions_stats (uuid, deaths, kills, played_games, wins, creation_date, update_date, played_time)";
                sql += " values (UNHEX('"+ Transcoder.Encode(player.getUuid().toString())+"'), ";
                sql += dimensionStats.getDeaths() + ", ";
                sql += dimensionStats.getKills() + ", ";
                sql += dimensionStats.getPlayedGames() + ", ";
                sql += dimensionStats.getWins() + ", ";
                sql += " now(), now(), ";
                sql += player.getPlayedTime() +")";

                // Execute the query
                statement.executeUpdate(sql);
            }
            else
            {
                // Set connection
                connection = dataSource.getConnection();
                statement = connection.createStatement();

                // Query construction for update
                String sql = "";
                sql += "update dimensions_stats set deaths=" + dimensionStats.getDeaths() + ", ";
                sql += "kills=" + dimensionStats.getKills() + ", ";
                sql += "played_games=" + dimensionStats.getPlayedGames() + ",";
                sql += "wins=" + dimensionStats.getWins() + ", ";
                sql += "update_date=now(),";
                sql += "played_time = played_time + " + player.getPlayedTime();
                sql += " where uuid=(UNHEX('"+ Transcoder.Encode(player.getUuid().toString())+"'))";

                // Execute the query
                statement.executeUpdate(sql);
            }

            // Set flag for nested query
        }
        catch(SQLException exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            // Close the query environment in order to prevent leaks
            close();
        }
    }

    // Get the board for this game
    public List<LeaderboardBean> getLeaderBoard(String category, DataSource dataSource)
    {
        List<LeaderboardBean> leaderBoard = new ArrayList<>();
        try
        {
            // Set connection
            connection = dataSource.getConnection();
            statement = connection.createStatement();

            // Query construction
            String sql = "select p.name as name, d." + category + " as score from players as p, dimensions_stats as d where p.uuid=d.uuid order by d." + category + " desc limit 3";

            // Execute the query
            resultset = statement.executeQuery(sql);

            // Manage the result in a bean
            while(resultset.next())
            {
                LeaderboardBean bean = new LeaderboardBean(resultset.getString("name"), resultset.getInt("score"));
                leaderBoard.add(bean);
            }
        }
        catch(SQLException exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            // Close the query environment in order to prevent leaks
            close();
        }
        return leaderBoard;
    }

    // Close all connection
    public void close()
    {
        // Close the query environment in order to prevent leaks
        try
        {
            if (resultset != null)
            {
                // Close the resulset
                resultset.close();
            }
            if (statement != null)
            {
                // Close the statement
                statement.close();
            }
            if (connection != null)
            {
                // Close the connection
                connection.close();
            }
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
    }
}
