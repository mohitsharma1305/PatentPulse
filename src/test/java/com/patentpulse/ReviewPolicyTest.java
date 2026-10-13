package com.patentpulse;

import static org.junit.jupiter.api.Assertions.*;

import com.patentpulse.service.ReviewPolicy;
import org.junit.jupiter.api.Test;

class ReviewPolicyTest {
  @Test
  void oneApprovalIsNotEnough() {
    assertEquals("IN_REVIEW", ReviewPolicy.status(1, 0));
  }

  @Test
  void twoApprovalsCompleteReview() {
    assertEquals("INTERNALLY_REVIEWED", ReviewPolicy.status(2, 0));
  }

  @Test
  void anyRejectionRequiresRevision() {
    assertEquals("NEEDS_REVISION", ReviewPolicy.status(2, 1));
  }

  @Test
  void finalStatesCannotReceiveFurtherVotes() {
    assertTrue(ReviewPolicy.terminal("NEEDS_REVISION"));
    assertTrue(ReviewPolicy.terminal("INTERNALLY_REVIEWED"));
    assertFalse(ReviewPolicy.terminal("IN_REVIEW"));
  }
}
