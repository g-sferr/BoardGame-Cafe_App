package it.unipi.dii.lsmsdb.boardgamecafe.repository.neo4jdbms;

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.neo4j.PostModelNeo4j;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.neo4j.UserModelNeo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.query.JSqlParserUtils;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class UserDBNeo4j {

    @Autowired
    UserRepoNeo4j userNeo4jDB;
    @Autowired
    Neo4jOperations neo4jOperations; //useful for aggregation


    public boolean addUser(UserModelNeo4j user) {
        try {
            userNeo4jDB.save(user);     // Same as performing a MERGE operation
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(String id, UserModelNeo4j updated) {
        try {
            Optional<UserModelNeo4j> old = userNeo4jDB.findById(id);
            if (old.isPresent()) {
                UserModelNeo4j oldUser = old.get();
                oldUser.setUsername(updated.getUsername());
                oldUser.setFollowedUsers(updated.getFollowedUsers());
                oldUser.setWrittenPosts(updated.getWrittenPosts());
                oldUser.setLikedPosts(updated.getLikedPosts());
                userNeo4jDB.save(oldUser);
                return true;
            }
            return false;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean deleteUserDetach(String username) {
        try {
            userNeo4jDB.deleteAndDetachUserByUsername(username); //Detach esplicito in repositoryINF
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Optional<UserModelNeo4j> findByUsername(String username) {
        Optional<UserModelNeo4j> user = Optional.empty();
        try {
            user = userNeo4jDB.findByUsername(username);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return user;
    }

    public void followUser(String following, String followed) {
        try {
            userNeo4jDB.addFollowRelationship(following, followed);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void unfollowUser(String unfollowing, String unfollowed) {
        try {
            userNeo4jDB.removeFollowRelationship(unfollowing, unfollowed);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getFollowedUsernames(String username) {
        List<String> followers = new ArrayList<>();
        try {
            return userNeo4jDB.findFollowedUsernamesByUsername(username);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return followers;
    }

    public int getCountFollowing(String username) {
        try {
            return userNeo4jDB.countFollowingByUsername(username);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getCountFollowers(String username) {
        try {
            return userNeo4jDB.countFollowersByUsername(username);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<String> getUsersByCommonBoardgamePosted(String username, int limit, int skipCounter)
    {
        List<String> suggestedUsers = new ArrayList<>();
        try { return userNeo4jDB.usersByCommonBoardgamePosted(username, limit, skipCounter); }
        catch (Exception e) { e.printStackTrace(); }
        return suggestedUsers;
    }

    public List<String> getUsersBySameLikedPosts(String username, int limit, int skipCounter)
    {
        List<String> suggestedUsers = new ArrayList<>();
        try { return userNeo4jDB.findUsersBySameLikedPosts(username, limit, skipCounter); }
        catch (Exception e) { e.printStackTrace(); }
        return suggestedUsers;
    }

    public List<String> getMostFollowedUsersUsernames(long minFollowersCount, int limit) {
        List<String> mostFollowedUsersUsernames = new ArrayList<>();
        try {
            return userNeo4jDB.findMostFollowedUsersUsernames(minFollowersCount, limit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mostFollowedUsersUsernames;
    }

    public Optional<UserModelNeo4j> findById(String id) {
        Optional<UserModelNeo4j> user = Optional.empty();
        try {
            user = userNeo4jDB.findById(id);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return user;
    }

    public boolean setUserAsBanned(String username) {
        try {
            userNeo4jDB.setUsernameAsBanned(username);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }


    public boolean restoreUserNodeAfterUnban(String userId, String username) {
        try {
            userNeo4jDB.restoreUserUsername(userId, userId);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}