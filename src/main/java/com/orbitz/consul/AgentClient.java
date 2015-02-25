package com.orbitz.consul;

import com.orbitz.consul.model.State;
import com.orbitz.consul.model.agent.*;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.Service;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * HTTP Client for /v1/agent/ endpoints.
 *
 * @see <a href="http://www.consul.io/docs/agent/http.html#agent">The Consul API Docs</a>
 */
public class AgentClient {

    private WebTarget webTarget;

    /**
     * Constructs an instance of this class.
     *
     * @param webTarget The {@link javax.ws.rs.client.WebTarget} to base requests from.
     */
    AgentClient(WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    /**
     * Indicates whether or not a particular service is registered with
     * the local Consul agent.
     *
     * @return <code>true</code> if a particular service is registered with
     * the local Consul agent, otherwise <code>false</code>.
     */
    public boolean isRegistered(String serviceId) {
        Map<String, Service> serviceIdToService = getServices();
        return serviceIdToService.containsKey(serviceId);
    }

    /**
     * Pings the Consul Agent.
     */
    public void ping() {
        try {
            Response.StatusType status = webTarget.path("self").request().get()
                    .getStatusInfo();

            if (status.getStatusCode() != Response.Status.OK.getStatusCode()) {
                throw new ConsulException(String.format("Error pinging Consul: %s",
                        status.getReasonPhrase()));
            }
        } catch (Exception ex) {
            throw new ConsulException("Error connecting to Consul", ex);
        }
    }

    /**
     * Registers the client as a service with Consul.  Registration enables
     * the use of checks.
     *
     * @param port The public facing port of the service to register with Consul.
     * @param ttl  Time to live for the Consul dead man's switch.
     * @param name Service name to register.
     * @param id   Service id to register.
     * @param tags Tags to register with.
     */
    public void register(int port, long ttl, String name, String id, String... tags) {
        register(port, CheckFactory.createTtlCheck(ttl), name, id, tags);
    }

    /**
     * Registers the client as a service with Consul.  Registration enables
     * the use of checks.
     *
     * @param port     The public facing port of the service to register with Consul.
     * @param script   Health script for Consul to use.
     * @param interval Health script run interval in seconds.
     * @param name     Service name to register.
     * @param id       Service id to register.
     * @param tags     Tags to register with.
     */
    public void register(int port, String script, long interval, String name, String id, String... tags) {
        register(port, CheckFactory.createScriptCheck(script, interval), name, id, tags);
    }

    /**
     * Registers the client as a service with Consul.  Registration enables
     * the use of checks.
     *
     * @param isHttp   Mark for the check type: true - HttpCheck, false - ScriptCheck
     * @param port     The public facing port of the service to register with Consul.
     * @param entity   Entity for registration (script ot http).
     * @param interval Interval in seconds.
     * @param name     Service name to register.
     * @param id       Service id to register.
     * @param tags     Tags to register with.
     */
    public void register(boolean isHttp, int port, String entity, long interval, String name, String id, String... tags) {
        Registration.Check check = isHttp ? CheckFactory.createHttpCheck(entity, interval) :
                CheckFactory.createScriptCheck(entity, interval);
        register(port, check, name, id, tags);
    }

    /**
     * Registers the client as a service with Consul.  Registration enables
     * the use of checks.
     *
     * @param port  The public facing port of the service to register with Consul.
     * @param check The health check to run periodically.  Can be null.
     * @param name  Service name to register.
     * @param id    Service id to register.
     * @param tags  Tags to register with.
     */
    public void register(int port, Registration.Check check, String name, String id, String... tags) {
        Registration registration = new Registration();

        registration.setPort(port);
        registration.setCheck(check);
        registration.setName(name);
        registration.setId(id);
        registration.setTags(tags);

        register(registration);
    }

    /**
     * Registers the client as a service with Consul.  Registration enables
     * the use of checks.
     *
     * @param registration The registration payload.
     */
    public void register(Registration registration) {
        Response response = webTarget.path("service").path("register").request()
                .put(Entity.entity(registration, MediaType.APPLICATION_JSON_TYPE));

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new ConsulException(response.readEntity(String.class));
        }
    }

    /**
     * De-register a particular service from the Consul Agent.
     */
    public void deregister(String serviceId) {
        Response response = webTarget.path("service").path("deregister").path(serviceId)
                .request().get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new ConsulException(response.readEntity(String.class));
        }
    }

    /**
     * Registers a Health Check with the Agent.
     *
     * @param checkId  The Check ID to use.  Must be unique for the Agent.
     * @param name     The Check Name.
     * @param script   Health script for Consul to use.
     * @param interval Health script run interval in seconds.
     */
    public void registerCheck(String checkId, String name, String script, long interval) {
        registerCheck(checkId, name, script, interval, null);
    }

    /**
     * Registers a Health Check with the Agent.
     *
     * @param checkId  The Check ID to use.  Must be unique for the Agent.
     * @param name     The Check Name.
     * @param script   Health script for Consul to use.
     * @param interval Health script run interval in seconds.
     * @param notes    Human readable notes.  Not used by Consul.
     */
    public void registerCheck(String checkId, String name, String script, long interval, String notes) {
        Check check = new Check();

        check.setId(checkId);
        check.setName(name);
        check.setScript(script);
        check.setInterval(String.format("%ss", interval));
        check.setNotes(notes);

        registerCheck(check);
    }

    /**
     * Registers a Health Check with the Agent.
     *
     * @param checkId The Check ID to use.  Must be unique for the Agent.
     * @param name    The Check Name.
     * @param ttl     Time to live for the Consul dead man's switch.
     */
    public void registerCheck(String checkId, String name, long ttl) {
        registerCheck(checkId, name, ttl, null);
    }

    /**
     * Registers a Health Check with the Agent.
     *
     * @param checkId The Check ID to use.  Must be unique for the Agent.
     * @param name    The Check Name.
     * @param ttl     Time to live for the Consul dead man's switch.
     * @param notes   Human readable notes.  Not used by Consul.
     */
    public void registerCheck(String checkId, String name, long ttl, String notes) {
        Check check = new Check();

        check.setId(checkId);
        check.setName(name);
        check.setTtl(String.format("%ss", ttl));
        check.setNotes(notes);

        registerCheck(check);
    }

    /**
     * Registers a Health Check with the Agent.
     *
     * @param check The Check to register.
     */
    public void registerCheck(Check check) {
        Response response = webTarget.path("check").path("register").request()
                .put(Entity.entity(check, MediaType.APPLICATION_JSON_TYPE));

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new ConsulException(response.readEntity(String.class));
        }
    }

    /**
     * De-registers a Health Check with the Agent
     *
     * @param checkId the id of the Check to deregister
     */
    public void deregisterCheck(String checkId) {
        Response response = webTarget.path("check").path("deregister").path(checkId)
                .request().get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new ConsulException(response.readEntity(String.class));
        }
    }

    /**
     * Retrieves the Agent's configuration and member information.
     * <p/>
     * GET /v1/agent/self
     *
     * @return The Agent information.
     */
    public Agent getAgent() {
        return webTarget.path("self").request().accept(MediaType.APPLICATION_JSON_TYPE)
                .get(Agent.class);
    }

    /**
     * Retrieves all checks registered with the Agent.
     * <p/>
     * GET /v1/agent/checks
     *
     * @return Map of Check ID to Checks.
     */
    public Map<String, HealthCheck> getChecks() {
        return webTarget.path("checks").request().accept(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<Map<String, HealthCheck>>() {
                });
    }

    /**
     * Retrieves all services registered with the Agent.
     * <p/>
     * GET /v1/agent/services
     *
     * @return Map of Service ID to Services.
     */
    public Map<String, Service> getServices() {
        return webTarget.path("services").request().accept(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<Map<String, Service>>() {
                });
    }

    /**
     * Retrieves all members that the Agent can see in the gossip pool.
     * <p/>
     * GET /v1/agent/members
     *
     * @return List of Members.
     */
    public List<Member> getMembers() {
        return webTarget.path("members").request().accept(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<Member>>() {
                });
    }

    /**
     * GET /v1/agent/force-leave/{node}
     * <p/>
     * Instructs the agent to force a node into the "left" state.
     *
     * @param node
     */
    public void forceLeave(String node) {
        webTarget.path("force-leave").path(node).request().get();
    }

    /**
     * Checks in with Consul.
     *
     * @param checkId The Check ID to check in.
     * @param state   The current state of the Check.
     * @param note    Any note to associate with the Check.
     */
    public void check(String checkId, State state, String note) throws NotRegisteredException {
        WebTarget resource = webTarget.path("check").path(state.getPath());

        if (note != null) {
            resource = resource.queryParam("note", note);
        }

        try {
            resource.path(checkId).request()
                    .get(String.class);
        } catch (InternalServerErrorException ex) {
            throw new NotRegisteredException();
        }
    }

    /**
     * Prepends the default TTL prefix to the serviceId to produce a check id,
     * then delegates to check(String checkId, State state, String note)
     * This method only works with TTL checks that have not been given a custom
     * name.
     *
     * @param serviceId
     * @param state
     * @param note
     * @throws NotRegisteredException
     */
    public void checkTtl(String serviceId, State state, String note) throws NotRegisteredException {
        check("service:" + serviceId, state, note);
    }

    /**
     * Sets a TTL check to "passing" state
     */
    public void pass(String checkId) throws NotRegisteredException {
        checkTtl(checkId, State.PASS, null);
    }

    /**
     * Sets a TTL check to "passing" state with a note
     */
    public void pass(String checkId, String note) throws NotRegisteredException {
        checkTtl(checkId, State.PASS, note);
    }

    /**
     * Sets a TTL check to "warning" state.
     */
    public void warn(String checkId) throws NotRegisteredException {
        checkTtl(checkId, State.WARN, null);
    }

    /**
     * Sets a TTL check to "warning" state with a note.
     */
    public void warn(String checkId, String note) throws NotRegisteredException {
        checkTtl(checkId, State.WARN, note);
    }

    /**
     * Sets a TTL check to "critical" state.
     */
    public void fail(String checkId) throws NotRegisteredException {
        checkTtl(checkId, State.FAIL, null);
    }

    /**
     * Sets a TTL check to "critical" state with a note.
     */
    public void fail(String checkId, String note) throws NotRegisteredException {
        checkTtl(checkId, State.FAIL, note);
    }
}
