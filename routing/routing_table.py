from l3addr import L3Addr
from icecream import ic

from l3interface import L3Interface

ic.enable()
# ic.disable()


class RoutingTableEntry:
    def __init__(self, iface_num: int, destaddr: L3Addr, mask_numbits: int, nexthop: L3Addr, is_local: bool):
        self.iface_num = iface_num
        self.destaddr = destaddr
        self.mask_numbits = mask_numbits
        self.is_local = is_local
        # if is_local is true, then nexthop value does not matter
        self.nexthop = nexthop


class RoutingTable:

    def __init__(self):
        self._entries = []

    def add_iface_route(self, iface_num: int, netaddr: L3Addr, mask_numbits: int, nexthop: L3Addr):
        '''Add a route out the given interface.
        Indicate a local route (no nexthop) by passing L3Addr("0.0.0.0") for nexthop'''

        is_local = nexthop.as_str() == "0.0.0.0"

        # Make sure the netaddr passed in is actually a network address -- host part is all 0s.
        netaddr = netaddr.network_part_as_L3Addr(mask_numbits)

        # Create a RoutingTableEntry and append to self._entries.
        entry = RoutingTableEntry(iface_num, netaddr, mask_numbits, nexthop, is_local)
        self._entries.append(entry)

    def add_route(self, ifaces: list, netaddr: L3Addr, mask_numbits: int, nexthop: L3Addr):
        '''Add a route. Indicate a local route (no nexthop) by passing L3Addr("0.0.0.0") for nexthop'''

        is_local = nexthop.as_str() == "0.0.0.0"

        # TODO: find the iface the nexthop address is accessible through.  raise ValueError if it
        # is not accessible out any interface. Store iface in out_iface.
        for iface in ifaces:
            if iface.on_same_network(nexthop): #! if the interface is on the same network as the next hop, it's accessible?
                out_iface = iface
        if out_iface == None:
            raise ValueError("Out iface not found")

        ic(str(out_iface))
        #! Make sure the destaddr passed in is actually a network address -- host part is all 0s.
        if not netaddr.host_part_as_int(mask_numbits) == 0:
            ic("Not a valid network address!")
            return

        ic("Adding route")
        #! Create routing table entry and add to list, similar to previous method.
        self._entries.append(RoutingTableEntry(out_iface.get_number(), netaddr, mask_numbits, nexthop, is_local))
    def __str__(self):
        ret = f"RoutingTable:\n"
        ret += f"netaddr   mask  nexthop   if\n"
        for e in self._entries:
            ret += f"{e.destaddr.as_str()}  {e.mask_numbits}    "
            ret += "local" if e.is_local else e.nexthop.as_str()
            ret += f"   {e.iface_num}"
            ret += "\n"
        return ret

    def get_best_route(self, dest: L3Addr) -> RoutingTableEntry:  # or None
        '''Use longest-prefix-match (LPM) to find and return the best route
        entry for the given dest address'''
        # TODO: return None if no matches (which means no default route)
        
        print("New call")
        longestMatch = None
        numLongestMatch = 0
        for entry in self._entries:
            # find out how many bits are matching
            num_prefix_bits = num_matching_prefix_bits(entry.destaddr, dest)
            ic(num_prefix_bits, entry.iface_num, entry.destaddr.as_str())
            # if that's more than the previous one, this one is the new longest
            ic(num_prefix_bits, numLongestMatch)
            if num_prefix_bits > numLongestMatch:
                longestMatch = entry
                numLongestMatch = num_prefix_bits
                ic(longestMatch.destaddr.as_str(), numLongestMatch)
                
        if longestMatch == None:
            return None
        else:
            ic(longestMatch.iface_num, longestMatch.destaddr.as_str())
            return longestMatch

def num_matching_prefix_bits(addr1: L3Addr, addr2: L3Addr) -> int:
    result = 0
    str1 = addr1.as_str()
    str2 = addr2.as_str()
    
    for i in range(0, len(str1)):
        if str1[i] == str2[i]:
            result += 1
        else: 
            return result
    return result

if __name__ == "__main__":
    r = RoutingTable()
    # This next route implies there is a 20.0.0.0 network attached.
    r.add_iface_route(1, L3Addr("30.0.0.0"), 8, L3Addr("20.0.0.2"))

    # Note: the following 2nd arg is not an actual *network* address,
    # so the add function should fix it automatically.
    # And the route is for a locally-attached network
    r.add_iface_route(2, L3Addr("10.0.0.1"), 24, L3Addr("0.0.0.0"))

    r.add_iface_route(3, L3Addr("10.1.2.3"), 8, L3Addr("10.0.78.89"))

    # default route
    r.add_iface_route(1, L3Addr("0.0.0.0"), 0, L3Addr("20.0.0.2"))

    print("Routing table looks like:\n", r)

    assert r.get_best_route(L3Addr("30.1.2.3")).iface_num == 1
    # Matches 2 routes, so should pick interface 2
    assert r.get_best_route(L3Addr("10.0.0.3")).iface_num == 2
    # Matches one route, with shorter prefix
    assert r.get_best_route(L3Addr("10.1.0.3")).iface_num == 3
    assert r.get_best_route(L3Addr("10.1.2.3")).iface_num == 3
    # default route
    assert r.get_best_route(L3Addr("192.168.2.1")).iface_num == 1

    print("Routing: all tests passed!")
