package io.github.kingg22.godot.api.annotations

import io.github.kingg22.godot.api.core.refcounted.MultiplayerAPI.RPCMode
import io.github.kingg22.godot.api.core.refcounted.MultiplayerPeer.TransferMode

/**
 * Mark the following method for remote procedure calls.
 *
 * If mode is set as "any_peer", allows any peer to call this RPC function. Otherwise,
 * only the authority peer is allowed to call it and mode should be kept as "authority".
 * When configuring functions as RPCs with Node.rpc_config(), each of these modes
 * respectively corresponds to the MultiplayerAPI.RPC_MODE_AUTHORITY and
 * MultiplayerAPI.RPC_MODE_ANY_PEER RPC modes.
 *
 * If sync is set as "call_remote", the function will only be executed on the remote peer,
 * but not locally. To run this function locally too, set sync to "call_local". When
 * configuring functions as RPCs with Node.rpc_config(), this is equivalent to setting
 * call_local to true.
 *
 * The transfer_mode accepted values are "unreliable", "unreliable_ordered", or "reliable".
 * It sets the transfer mode of the underlying MultiplayerPeer.
 *
 * The transfer_channel defines the channel of the underlying MultiplayerPeer.
 *
 * Note: Methods annotated with @rpc cannot receive objects which define required parameters
 * in Object._init().
 *
 * @param mode The RPC mode ("authority" or "any_peer").
 * @param sync The synchronization mode ("call_remote" or "call_local").
 * @param transferMode The transfer mode ("unreliable", "unreliable_ordered", or "reliable").
 * @param transferChannel The transfer channel to use.
 */
@Retention(SOURCE)
@Target(FUNCTION)
@MustBeDocumented
public annotation class Rpc(
    public val mode: RPCMode = AUTHORITY,
    public val sync: SyncMode = CALL_REMOTE,
    public val transferMode: TransferMode = RELIABLE,
    public val transferChannel: Int = 0,
) {
    public enum class SyncMode { CALL_REMOTE, CALL_LOCAL; }
}
