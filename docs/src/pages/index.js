/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import React from "react";
import clsx from "clsx";
import Layout from "@theme/Layout";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import useBaseUrl from "@docusaurus/useBaseUrl";
import styles from "./styles.module.css";
import "./index.scss";
import CodeBlock from "@theme/CodeBlock";
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";

const features = [
  {
    newline: "",
    title: "New Model Defination",
    description: (
      <>
        <b>SWMP</b> is design for MLOps.
      </>
    ),
  },
  {
    newline: "",
    title: "New Dataset Defination",
    description: (
      <>
        <b>SWDS</b> is design for MLOps.
      </>
    ),
  },
  {
    newline: "",
    title: "Model Evaluation MLOps",
    description: <>Model Evaluation is local or cluster.</>,
  },
  {
    newline: "",
    title: "Cluster Scaliabilty",
    description: <>Easy to scale in cluster.</>,
  },
  {
    newline: "",
    title: "Easy Deployment",
    description: (
      <>
        One click <b>Model Evaluation</b> to your production Kubernetes or
        BareMental Cluster <b>directly from the UI</b>.
      </>
    ),
  },
  {
    newline: "",
    title: "Zero Ops",
    description: <>No ops.</>,
  },
];

const sectionUsers = [
  {
    image: "img/swmp.png",
    title: "Model Defination",
    description:
      "Text space occupation of detailed descriptionText space occupation of detailedscriptionText space occupation of detailscriptionText space occupation of detailscriptionText space occupation of detailscriptionText space occupation of detail",
  },
  {
    image: "img/swmp.png",
    title: "Model Defination",
    description:
      "Text space occupation of detailed descriptionText space occupation of detailed",
  },
  {
    image: "img/swmp.png",
    title: "Model Defination",
    description:
      "Text space occupation of detailed descriptionText space occupation of detailed",
  },
];

function Feature({ imageUrl, title, description, newline }) {
  const imgUrl = useBaseUrl(imageUrl);
  return (
    <div
      className={clsx(
        `col col--4 col--feature text--justified  ${newline}`,
        styles.feature
      )}
    >
      {imgUrl && (
        <div>
          <img className={styles.featureImage} src={imgUrl} alt={title} />
        </div>
      )}
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}

function Home() {
  const context = useDocusaurusContext();
  const { siteConfig = {} } = context;
  return (
    <Layout
      title={`Hello from ${siteConfig.title}`}
      description="Description will go into a meta tag in <head />"
    >
      <header className={clsx("hero hero--primary", styles.heroBanner)}>
        <div className="container">
          <h1 className={styles.heroProjectTagline}>
            {/* <img
              alt={siteConfig.title}
              className={styles.heroLogo}
              src={useBaseUrl("/img/starwhale-white.png")}
            /> */}
            <span
              className={styles.heroTitleTextHtml}
              dangerouslySetInnerHTML={{
                __html: siteConfig.tagline,
              }}
            />
          </h1>
          <div className={styles.indexCtas}>
            <Link
              className="button button--primary button--secondary button--lg button--rounded"
              to="/docs"
            >
              Get started
            </Link>
            <Link
              className="button button--outline button--secondary button--lg button--rounded"
              to="https://app.starwhale.ai"
            >
              Request a Demo
            </Link>
            {/* <span className={styles.indexCtasGitHubButtonWrapper}>
              <iframe
                className={styles.indexCtasGitHubButton}
                src="https://ghbtns.com/github-btn.html?user=star-whale&amp;repo=starwhale&amp;type=star&amp;count=true&amp;size=large"
                width={160}
                height={30}
                title="GitHub Stars"
              />
            </span> */}
          </div>
        </div>
      </header>
      <main className="swMain">
        {/* starwhale tabs show case of project/model/dataset/runtime */}
        <section className="card swTabs">
          <div className="card__body">
            <Tabs className="tabs tabs--block">
              <TabItem value="apple" label="Apple" default>
                <div className="tabs__body">
                  <code className="tabs__code" id="project">
                    （输入comand生成dataset示意图）
                    <p>1 # MNIST目录下，根据dataset.yaml内容，构建swds </p>
                    <p>2 # dataset.yaml 内容可以按需修改</p> 4{" "}
                    <p>5 # 查看构建的swds</p>{" "}
                    <p>6 swcli dataset list[project uri] </p>7
                    <p> 8 # push swds到controller中</p>
                    <p>
                      9 swcli dataset push mnist:hbsgeyzxmq4deytfgy3gin3bhbrxo5a
                    </p>
                  </code>
                  <div className="tabs__desc " id="project">
                    <span>
                      - starwhale public dataset- starwhale public dataset-
                      starwhale public dataset - starwhale public dataset -
                      starwhale public dataset- starwhale public dataset-
                      starwhale public dataset- starwhale public dataset-
                      starwhale public dataset- starwhale public dataset-
                      starwhale public dataset - starwhale public dataset-
                      starwhale public dataset- starwhale public dataset
                    </span>
                    <Link
                      className="button button--primary  button--rounded"
                      to="/docs"
                    >
                      Learn more
                    </Link>
                  </div>
                </div>
              </TabItem>
              <TabItem value="orange" label="Orange">
                <div className="tabs__body"></div>
              </TabItem>
              <TabItem value="banana" label="Banana">
                <div className="tabs__body"></div>
              </TabItem>
            </Tabs>
            {/* <ul className="tabs tabs--block">
              <li id="project" className="tabs__item tabs__item--active">
                Starwhale Project
              </li>
              <li id="model" className="tabs__item">
                Starwhale Model
              </li>
              <li id="dataset" className="tabs__item">
                Starwhale Dataset
              </li>
              <li id="runtime" className="tabs__item">
                Starwhale Runtime
              </li>
            </ul>
            <div className="tabs__body">
              <code className="tabs__code" id="project">
                （输入comand生成dataset示意图）
                <p>1 # MNIST目录下，根据dataset.yaml内容，构建swds </p>
                <p>2 # dataset.yaml 内容可以按需修改</p> 4{" "}
                <p>5 # 查看构建的swds</p>{" "}
                <p>6 swcli dataset list[project uri] </p>7
                <p> 8 # push swds到controller中</p>
                <p>
                  9 swcli dataset push mnist:hbsgeyzxmq4deytfgy3gin3bhbrxo5a
                </p>
              </code>
              <div className="tabs__desc " id="project">
                <span>
                  - starwhale public dataset- starwhale public dataset-
                  starwhale public dataset - starwhale public dataset -
                  starwhale public dataset- starwhale public dataset- starwhale
                  public dataset- starwhale public dataset- starwhale public
                  dataset- starwhale public dataset- starwhale public dataset -
                  starwhale public dataset- starwhale public dataset- starwhale
                  public dataset
                </span>
                <Link
                  className="button button--primary  button--rounded"
                  to="/docs"
                >
                  Learn more
                </Link>
              </div>
            </div> */}
          </div>
        </section>
        {/* starwhale users */}
        <section className="swUser">
          <h1>Who is Starwhale for?</h1>
          <div className="divider">Icon</div>
          <div className="user__body">
            {sectionUsers.map((user, index) => (
              <div className="card" key={index}>
                <div className="card__image">
                  <div
                    style={{
                      backgroundImage: `url(${user.image})`,
                    }}
                    alt="Image alt text"
                  />
                </div>
                <div className="card__body">
                  <h3>{user.title}</h3>
                  <div className="divider2">———— —— -</div>
                  <span>{user.description}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
        {/* starwhale try now */}
        <section className="swIntegrate">
          <h1>Starwhale integrates with any framework</h1>
          <div className="divider">Icon</div>
        </section>
        <section className="swTry">
          <h1>Try today</h1>
          <div className="try">
            <div className="try__item">
              <span>
                <b>If</b> you want to know more detail about starwhale just
                click the button "get started"ant to know more detail about
                starwhale just click the button "get started"ant to know more
                detail about starwhale just click the button "get started"ant to
                know more detail about starwhale just click the button "get
                started"
              </span>
              <Link
                className="button button--primary  button--rounded"
                to="/docs"
              >
                Get started
              </Link>
            </div>
            <div className="try__item">
              <span>- starwhale public dataset</span>
              <Link
                className="button button--primary  button--rounded"
                to="/docs"
              >
                Request a Demo
              </Link>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}

export default Home;
