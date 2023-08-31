package kr.kro.tripsketch.services

import kr.kro.tripsketch.domain.Trip
import kr.kro.tripsketch.dto.TripCreateDto
import kr.kro.tripsketch.dto.TripDto
import kr.kro.tripsketch.dto.TripUpdateDto
import kr.kro.tripsketch.repositories.FollowRepository
import kr.kro.tripsketch.repositories.TripRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val followRepository: FollowRepository,
    private val jwtService: JwtService,
    private val userService: UserService,
    private val tripLikeService: TripLikeService,
    private val notificationService: NotificationService
) {
    fun createTrip(email: String, tripCreateDto: TripCreateDto): TripDto {
        val newTrip = Trip( 
            email = email,
            title = tripCreateDto.title,
            content = tripCreateDto.content,
            location = tripCreateDto.location,
            startedAt = tripCreateDto.startedAt,
            endAt = tripCreateDto.endAt,
            hashtag = tripCreateDto.hashtag,
            public = tripCreateDto.public,
            images = tripCreateDto.images
        )
        val createdTrip = tripRepository.save(newTrip)
        // 나를 팔로우하는 사람들에게 알람 보내기 기능
//        val followerEmails = followRepository.findByFollowing(email)
        val followers = followRepository.findByFollowing(email)
        val filteredFollowers = followers.filter { it.follower != email }
        val followerEmails = filteredFollowers.map { it.follower }
        val followingNickname = userService.findUserByEmail(email)?.nickname ?: "Unknown user"

        notificationService.sendPushNotification(
            followerEmails,
            "새로운 여행의 시작, 트립스케치",
            "$followingNickname 님이 새로운 글을 작성하였습니다.",
            null,
            null,
            createdTrip.id,
            followingNickname
        )
        return fromTrip(createdTrip, email,false)
    }

    fun getAllTrips(email: String): Set<TripDto> {
        val findTrips = tripRepository.findAll()
        return findTrips.map { fromTrip(it, email, false) }.toSet()
    }

    fun getAllTripsByUser(email: String): Set<TripDto> {
//        val findTrips = tripRepository.findByHiddenIsFalse()
//        return findTrips.map { fromTrip(it, email,false) }.toSet()

        val findTrips: Set<Trip> = if (email.isNotEmpty()) {
            // to-do : 매개변수 이메일과 findTrips 에서의 email 과 동일하다면 비공개 포함하여 보여줌 - findByHiddenIsFalse
            tripRepository.findTripByEmail(email) +
                    tripRepository.findByPublicIsTrueAndHiddenIsFalse()
        } else {
            // to-do : 같지않다면 공개 게시물만 보여줌 - findByPublicIsTrueAndHiddenIsFalse
            tripRepository.findByPublicIsTrueAndHiddenIsFalse()
        }
        return findTrips.map { fromTrip(it, email, false) }.toSet()
    }

    fun getAllTripsByGuest(): Set<TripDto> {
        val findTrips = tripRepository.findByPublicIsTrueAndHiddenIsFalse()
            ?: throw IllegalArgumentException("작성된 게시글이 존재하지 않습니다.")
        return findTrips.map { fromTrip(it, "",false) }.toSet()
    }

    fun getTripByNickname(nickname: String): Set<TripDto> {
        val user = userService.findUserByNickname(nickname)
//        val findTrips = tripRepository.findTripByEmail(user!!.email)
        val findTrips = tripRepository.findTripByEmailAndHiddenIsFalse(user!!.email)
//            ?: throw IllegalArgumentException("작성한 게시글이 존재하지 않습니다.")
        return findTrips.map { fromTrip(it, "",false) }.toSet()
    }

//    fun getTripByNickname(nickname: String, pageable: Pageable): Page<TripDto> {
//        val user = userService.findUserByNickname(nickname)
//        val findTrips = tripRepository.findTripByEmail(user!!.email, pageable)
////        return findTrips.map { fromTrip(it, false) }.toSet()
//        return findTrips.map { fromTrip(it, false) }.let { PageImpl(it.toList(), pageable, findTrips.totalElements) }
//
//    }

    fun getTripByEmailAndId(email: String, id: String): TripDto? {
//        val findTrip = tripRepository.findById(id).orElse(null)
        val findTrip = tripRepository.findByIdAndHiddenIsFalse(id)
        ?: throw IllegalArgumentException("해당 게시글이 존재하지 않습니다.")

        // 조회수
        if (!findTrip.tripViews.contains(email) && findTrip.email != email ) {
            findTrip.tripViews.add(email)
            findTrip.views += 1
            tripRepository.save(findTrip)
        }
        return fromTrip(findTrip, email,false)
    }


    fun getTripById(id: String): TripDto? {
//        val findTrip = tripRepository.findById(id).orElse(null)
        val findTrip = tripRepository.findByIdAndHiddenIsFalse(id)
            ?: throw IllegalArgumentException("해당 게시글이 존재하지 않습니다.")
        return fromTrip(findTrip, "",false)
    }

    // to-do : (메인페이지-모바일(회원))내가 구독한 여행자의 스케치(following 한 nickname 에 대한 카드 1개씩 조회 - 카드 갯수는 설정할 수 있게끔 하자)
    // 구독 유무를 변수로 받아줄 수 있으면 그렇게 하자.


    // to-do : (마이페이지) 카테고리(hashtag) + 닉네임으로 조회
    // 구독 유무를 변수로 받아줄 수 있으면 그렇게 하자.


    // to-do : (메인, 탐색 페이지) 검색어 +  요즘 인기있는 게시물 조회(구독과 상관없이 - 카드 갯수는 설정할 수 있게끔 하자)
    // 구독 유무를 변수로 받아줄 수 있으면 그렇게 하자.
    // 인기있는 게시물은 어떻게 따지나? (기준 - 조회수, 좋아요 갯수, comment 갯수 있음)


    // to-do : (탐색 페이지) 검색어 + 등록일 기준 최신순으로 게시물 조회(구독과 상관없이 - 카드 갯수는 설정할 수 있게끔 하자)
    // 구독 유무를 변수로 받아줄 수 있으면 그렇게 하자.


    fun updateTrip(email: String, tripUpdateDto: TripUpdateDto): TripDto {
//        val findTrip = tripRepository.findById(tripUpdateDto.id!!).orElseThrow {
        val findTrip = tripRepository.findById(tripUpdateDto.id).orElseThrow {
            EntityNotFoundException("수정할 게시글이 존재하지 않습니다.")
        }
        if (findTrip.email == email) {
            findTrip.apply {
                title = tripUpdateDto.title
                content = tripUpdateDto.content
                location = tripUpdateDto.location
                startedAt = tripUpdateDto.startedAt ?: startedAt
                endAt = tripUpdateDto.endAt ?: endAt
                hashtag = tripUpdateDto.hashtag
                public = tripUpdateDto.public
//                updatedAt = LocalDateTime.now()
                images = tripUpdateDto.images
            }
            val updatedTrip = tripRepository.save(findTrip)
            return fromTrip(updatedTrip, "", false)
        } else {
            throw IllegalAccessException("수정할 권한이 없습니다.")
        }
    }


    fun deleteTripById(email: String, id: String): Unit {
        val findTrip = tripRepository.findById(id).orElseThrow {
            EntityNotFoundException("삭제할 게시글이 존재하지 않습니다.")
        }
        if (findTrip.email == email) {
            findTrip.hidden = true
            findTrip.deletedAt = LocalDateTime.now()
            tripRepository.save(findTrip)
        } else {
            throw IllegalAccessException("삭제할 권한이 없습니다.")
        }
    }


    fun toTrip(tripDto: TripDto): Trip {
        return Trip(
            id = tripDto.id,
            email = tripDto.email!!,
            title = tripDto.title,
            content = tripDto.content,
            likes = tripDto.likes!!,
            views = tripDto.views!!,
            location = tripDto.location,
            startedAt = tripDto.startedAt,
            endAt = tripDto.endAt,
            hashtag = tripDto.hashtag,
            public = tripDto.public,
            hidden = tripDto.hidden,
            createdAt = tripDto.createdAt,
            updatedAt = tripDto.updatedAt,
            deletedAt = tripDto.deletedAt,
            tripLikes = tripDto.tripLikes,
//            tripViews = tripDto.tripViews,
            images = tripDto.images
        )
    }

    fun fromTrip(trip: Trip, currentUserEmail: String, includeEmail: Boolean = true): TripDto {
        val user = userService.findUserByEmail(trip.email)
        val isLiked = trip.tripLikes.contains(currentUserEmail)
        return if (includeEmail) {
            TripDto(
                id = trip.id,
                email = trip.email,
                nickname = user!!.nickname,
                title = trip.title,
                content = trip.content,
                likes = trip.likes,
                views = trip.views,
                location = trip.location,
                startedAt = trip.startedAt!!,
                endAt = trip.endAt!!,
                hashtag = trip.hashtag,
                public = trip.public ?: true,
                hidden = trip.hidden,
                createdAt = trip.createdAt,
                updatedAt = trip.updatedAt,
                deletedAt = trip.deletedAt,
                tripLikes = trip.tripLikes,
//                tripViews = trip.tripViews,
                isLiked = isLiked,
                images = trip.images
            )
        } else {
            TripDto(
                id = trip.id,
                email = null,
                nickname = user!!.nickname,
                title = trip.title,
                content = trip.content,
                likes = trip.likes,
                views = trip.views,
                location = trip.location,
                startedAt = trip.startedAt!!,
                endAt = trip.endAt!!,
                hashtag = trip.hashtag,
                public = trip.public ?: true,
                hidden = trip.hidden,
                createdAt = trip.createdAt,
                updatedAt = trip.updatedAt,
                deletedAt = trip.deletedAt,
                tripLikes = trip.tripLikes,
//                tripViews = trip.tripViews,
                isLiked = isLiked,
                images = trip.images
            )
        }
    }

}
