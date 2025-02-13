package com.webscraper.services;


import crawlercommons.robots.BaseRobotRules;

public interface RobotsTxtService {
    BaseRobotRules getRules(String domain);
    boolean isAllowed(String url, BaseRobotRules rules);
}
