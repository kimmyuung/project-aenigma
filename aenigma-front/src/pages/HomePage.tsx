import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Header } from '../components/Header';
import './HomePage.css';

export function HomePage() {
    const { isAuthenticated } = useAuth();

    return (
        <div className="home-page">
            <Header />

            <main className="home-main">
                <div className="hero-section">
                    <div className="hero-glow"></div>
                    <div className="hero-content animate-fadeIn">
                        <span className="hero-badge">🎭 Murder Mystery Game</span>
                        <h1 className="hero-title">
                            <span className="gradient-text">AENIGMA</span>
                        </h1>
                        <p className="hero-subtitle">
                            친구들과 함께 즐기는 온라인 마피아 추리 게임
                            <br />
                            범인을 찾아내거나, 끝까지 숨어라!
                        </p>

                        <div className="hero-actions">
                            {isAuthenticated ? (
                                <Link to="/rooms" className="btn btn-primary btn-lg">
                                    🎮 게임 시작하기
                                </Link>
                            ) : (
                                <>
                                    <Link to="/register" className="btn btn-primary btn-lg">
                                        시작하기
                                    </Link>
                                    <Link to="/login" className="btn btn-secondary btn-lg">
                                        로그인
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>
                </div>

                <section className="features-section">
                    <h2 className="section-title">게임 특징</h2>
                    <div className="features-grid">
                        <FeatureCard
                            icon="🕵️"
                            title="역할 기반 게임플레이"
                            description="탐정, 범인, 용의자 등 다양한 역할을 맡아 추리와 속임수의 게임을 즐기세요."
                        />
                        <FeatureCard
                            icon="💬"
                            title="실시간 채팅"
                            description="귓속말, 공개 채팅, 범인 전용 채팅 등 다양한 소통 방식을 제공합니다."
                        />
                        <FeatureCard
                            icon="🎙️"
                            title="Discord 연동"
                            description="Discord 음성 채널과 연동하여 더욱 몰입감 있는 게임을 즐기세요."
                        />
                        <FeatureCard
                            icon="⚡"
                            title="실시간 진행"
                            description="GM이 Discord에서 게임을 진행하고, 웹에서 실시간으로 참여하세요."
                        />
                    </div>
                </section>

                <section className="howto-section">
                    <h2 className="section-title">게임 방법</h2>
                    <div className="steps-container">
                        <Step number={1} title="방 만들기" description="새 방을 만들거나 친구의 방에 입장하세요." />
                        <Step number={2} title="역할 배정" description="GM이 게임을 시작하면 비밀 역할이 배정됩니다." />
                        <Step number={3} title="조사 단계" description="단서를 수집하고 다른 플레이어와 대화하세요." />
                        <Step number={4} title="최종 투표" description="범인을 지목하거나, 의심을 피해 생존하세요." />
                    </div>
                </section>
            </main>

            <footer className="home-footer">
                <p>© 2024 AENIGMA. Built for fun.</p>
            </footer>
        </div>
    );
}

function FeatureCard({ icon, title, description }: { icon: string; title: string; description: string }) {
    return (
        <div className="feature-card card">
            <span className="feature-icon">{icon}</span>
            <h3 className="feature-title">{title}</h3>
            <p className="feature-description">{description}</p>
        </div>
    );
}

function Step({ number, title, description }: { number: number; title: string; description: string }) {
    return (
        <div className="step">
            <div className="step-number">{number}</div>
            <div className="step-content">
                <h4 className="step-title">{title}</h4>
                <p className="step-description">{description}</p>
            </div>
        </div>
    );
}
