
def maskToInt(mask: int) -> int:
    '''Convert a mask length (e.g., 24) into the 32-bit value (e.g., 24 1 bits
    followed by 8 0 bits.'''
    return ((2 ** mask) - 1) << (32 - mask)


def maskToHostMask(mask_numbits: int) -> int:
    '''Convert a network mask (e.g., 24) into the 32-bit value where only the
    host part has 1 bits.'''
    # Top (32 - mask_numbits) bits are 0, followed by all 1.
    return 2 ** (32 - mask_numbits) - 1


if __name__ == "__main__":
    assert maskToHostMask(24) == 0b11111111
    assert maskToHostMask(8) == 0b11111111_11111111_11111111
    print("Utils: all tests passed")
